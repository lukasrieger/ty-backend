package repository.extensions

import arrow.core.Either
import arrow.core.Either.Companion.left
import arrow.core.Either.Companion.right
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.either.applicativeError.raiseError
import arrow.core.extensions.either.monad.flatMap
import arrow.core.extensions.either.monad.forEffect
import arrow.core.extensions.either.monadError.ensure
import arrow.core.extensions.fx
import arrow.core.extensions.list.traverse.sequence
import arrow.core.extensions.option.applicative.applicative
import arrow.fx.IO
import arrow.fx.extensions.fx
import kotlinx.coroutines.Dispatchers
import model.Article
import model.recurrentCopy
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.joda.time.DateTime
import repository.*
import repository.dao.ArticlesTable


internal suspend fun queryPaginate(
    query: Query,
    limit: Int? = null,
    offset: Int? = null,
    ordering: () -> Pair<Column<DateTime>, SortOrder> = { ArticlesTable.applicationDeadline to SortOrder.ASC }
) = newSuspendedTransaction(Dispatchers.IO) {
    query.paginate(limit, offset)
        .orderBy(ordering())
}

/**
 * This function behaves exactly like [Repository.byQuery], only that only archived articles will be returned by this
 * function. Note that this also changes the ordering of the resulting articles. Those articles whose archiveDate is
 * closest to the current Date will be closer to the top.
 * @receiver Repository<Article>
 * @param limit Int?
 * @param offset Int?
 * @param query Query
 * @return QueryResult<Article>
 */
suspend fun Reader<Article>.byQueryArchived(limit: Int?, offset: Int?, query: Query): QueryResult<Article> {
    val count = countOf(query)

    val queryResult = queryPaginate(query, limit, offset) {
        ArticlesTable.archiveDate to SortOrder.DESC
    }
        .andWhere { ArticlesTable.archiveDate less DateTime.now() }
        .map { it.toArticle() }

    return QueryResult(count, queryResult)
}


private suspend fun updateArticle(id: Int, statement: UpdateStatement.() -> Unit): Result<ArticleIndex> = Either.catch {
    newSuspendedTransaction(Dispatchers.IO) {
        ArticlesTable.run {
            update({ ArticlesTable.id eq id }) {
                it.run(statement)
            }
        }
    }
}.map (::keyOf)


/**
 * TODO
 * @receiver Repository<Article>
 * @return Result<Unit>
 */
suspend fun Writer<Article>.createRecurrentArticles(): Result<Unit> =
    newSuspendedTransaction(Dispatchers.IO) {
        ArticlesTable.select {

            (ArticlesTable.isRecurrent eq true) and
                    (ArticlesTable.applicationDeadline lessEq DateTime.now()) and
                    ArticlesTable.childArticle.isNull()

        }
            .map {
                val article = it.toArticle()
                article to article.recurrentCopy()
            }
    }
        .map { (parent, child) ->
            val (parentKey) = parent.id
            val (childKey) = child.id


            create(Valid(child)).fold(
                ifLeft = ::left,
                ifRight = {
                    updateArticle(parentKey) {
                        this[ArticlesTable.childArticle] = childKey
                        this[ArticlesTable.isRecurrent] = false
                    }
                }
            )

        }
        .sequence(Either.applicative()).fix() // Turns List<Either<L,R>> into Either<L,List<R>>
        .fold(
            ifLeft = ::left,
            ifRight = { right(Unit) }
        )


private fun Query.paginate(limit: Int?, offset: Int?): Query = apply {
    if (limit != null) {
        if (offset != null) {
            limit(limit, offset)
        }
        limit(limit)
    }
}



