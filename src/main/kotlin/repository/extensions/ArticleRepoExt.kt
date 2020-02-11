package repository.extensions

import arrow.core.Either
import arrow.core.Either.Companion.left
import arrow.core.Right
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.list.traverse.sequence
import arrow.core.fix
import kotlinx.coroutines.Dispatchers
import model.Article
import model.error.leftOf
import model.recurrentCopy
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.joda.time.DateTime
import repository.*
import repository.dao.ArticlesTable


internal suspend fun queryResultSet(
    query: Query,
    limit: Int? = null,
    offset: Int? = null,
    ordering: Ordering<DateTime, SortOrder> = orderOf { ArticlesTable.applicationDeadline to SortOrder.ASC }
) = newSuspendedTransaction(Dispatchers.IO) {
    query.paginate(limit, offset)
        .orderBy(ordering.ord)
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
suspend fun ReadableRepository<Article>.byQueryArchived(limit: Int?, offset: Int?, query: Query): QueryResult<Article> =
    (countOf(query) to queryResultSet(query, limit, offset, orderOf {
        ArticlesTable.archiveDate to SortOrder.DESC
    })
        .andWhere { ArticlesTable.archiveDate less DateTime.now() }
        .map { it.toArticle() }
            ).let { (count, seq) -> QueryResult(count, seq) }


private suspend fun updateArticle(id: Int, statement: UpdateStatement.() -> Unit): Result<ArticleIndex> =
    newSuspendedTransaction(Dispatchers.IO) {
        ArticlesTable.runCatching {
            update({ ArticlesTable.id eq id }) {
                it.run(statement)
            }
        }.mapCatching {
            keyOf<Article>(it)
        }.fold(
            onSuccess = ::Right,
            onFailure = { leftOf<ArticleIndex>(it) }
        )

    }

/**
 * This function is rather complex.
 * As a first step, this function queries all articles for which the recurrence date is (as of DateTime.now())
 * in the past.
 * These articles are subsequently mapped to a pair of themselves and their recurrent copy.
 * All that is left now is actually writing the new child articles to the database. The problem is though, that
 * updating and creating are inherently fallible actions.
 * (While that is also theoretically the case for simply reading from the database, I chose to model reading to be
 * safe as a design decision)
 *
 * We achieve complete exception safety by wrapping all actions in the result type.
 * Note that we have to use when in [map] in order to stay within the coroutine context of [Repository]
 * @receiver Repository<Article>
 * @return Result<Unit>
 */
suspend fun WriteableRepository<Article>.createRecurrentArticles(): Result<Unit> =
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

            when (val recurrentResult = create(child)) {
                is Either.Left -> recurrentResult
                is Either.Right -> updateArticle(parentKey) {
                    this[ArticlesTable.childArticle] = childKey
                    this[ArticlesTable.isRecurrent] = false
                }

            }
        }
        .sequence(Either.applicative()).fix() // Turns List<Either<L,R>> into Either<L,List<R>>
        .fold(
            ifLeft = ::left,
            ifRight = { Result.right(Unit) }
        )

private fun Query.paginate(limit: Int?, offset: Int?): Query = apply {
    if (limit != null) {
        if (offset != null) {
            limit(limit, offset)
        }
        limit(limit)
    }
}

