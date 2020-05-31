package repository.extensions

import arrow.core.*
import arrow.core.Either.Companion.left
import arrow.core.Either.Companion.right
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.list.traverse.sequence
import kotlinx.coroutines.Dispatchers
import model.Article
import model.extensions.fromResultRow
import model.recurrentCopy
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.joda.time.DateTime
import repository.*
import repository.dao.ArticlesTable
import validation.ArticleValidator


internal fun queryPaginate(
    query: Query,
    limit: Int? = null,
    offset: Long? = null,
    ordering: () -> Pair<Column<DateTime>, SortOrder> = { ArticlesTable.applicationDeadline to SortOrder.ASC }
): Query = query.paginate(limit, offset)
    .orderBy(ordering())


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
suspend fun Reader<Article>.byQueryArchived(
    query: Query,
    limit: Int?,
    offset: Long?
): Either<Throwable, QueryResult<Article>> = with(Article.fromResultRow) {
    countOf(query).fold(
        ifLeft = ::Left,
        ifRight = { count ->
            val queryResult = queryPaginate(query, limit, offset) {
                ArticlesTable.archiveDate to SortOrder.DESC
            }
                .andWhere { ArticlesTable.archiveDate less DateTime.now() }
                .map { it.coerce() }

            Right(QueryResult(count, queryResult))
        }
    )
}


private suspend fun <T> Writer<T>.updateArticle(
    id: Int,
    statement: UpdateStatement.() -> Unit
): Either<Throwable, ArticleIndex> =
    transactionContext(ArticlesTable) {
        update({ ArticlesTable.id eq id }) {
            it.run(statement)
        }
    }.map(::keyOf)


/**
 * @receiver Repository<Article>
 * @return Result<Unit>
 */
suspend fun Writer<Article>.createRecurrentArticles(): Either<Throwable, Unit> = with(Article.fromResultRow) {
    newSuspendedTransaction(Dispatchers.IO) {
        ArticlesTable.select {

            (ArticlesTable.isRecurrent eq true) and
                    (ArticlesTable.applicationDeadline lessEq DateTime.now()) and
                    ArticlesTable.childArticle.isNull()

        }.map { it.coerce() }
    }.map { parent ->

        val parentKey = parent.id.key
        val child = parent.recurrentCopy()
        val childKey = child.id.key

        when (val check = ArticleValidator.validate(child)) {
            is Validated.Valid -> create(check).fold(
                ifLeft = ::left,
                ifRight = {
                    updateArticle(parentKey) {
                        this[ArticlesTable.childArticle] = childKey
                        this[ArticlesTable.isRecurrent] = false
                    }
                }
            )
            is Validated.Invalid -> left(Throwable("Child of article $parentKey cannot be validated."))
        }
    }
        .sequence(Either.applicative()).fix()
        .fold(
            ifLeft = ::left,
            ifRight = { right(Unit) }
        )

}


internal fun Query.paginate(limit: Int?, offset: Long?): Query = apply {
    if (limit != null) {
        if (offset != null) {
            limit(limit, offset)
        }
        limit(limit)
    }
}


