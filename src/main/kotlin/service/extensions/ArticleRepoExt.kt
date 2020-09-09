package service.extensions

import arrow.core.Either
import arrow.core.Validated
import arrow.core.computations.either
import arrow.core.left
import arrow.fx.coroutines.parTraverse
import model.Article
import model.recurrentCopy
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.joda.time.DateTime
import service.*
import service.dao.ArticlesTable


internal fun queryPaginate(
    query: Query,
    limit: Int? = null,
    offset: Long? = null,
    ordering: () -> Pair<Column<DateTime>, SortOrder> = { ArticlesTable.applicationDeadline to SortOrder.ASC }
): Query =
    query.paginate(limit, offset)
        .orderBy(ordering())


/**
 * This function behaves exactly like [Service.byQuery], only that only archived articles will be returned by this
 * function. Note that this also changes the ordering of the resulting articles. Those articles whose archiveDate is
 * closest to the current Date will be closer to the top.
 * @receiver Repository<Article>
 * @param limit Int?
 * @param offset Int?
 * @param query Query
 * @return QueryResult<Article>
 */
suspend fun <E> Reader<*, E, Article>.byQueryArchived(
    query: Query,
    limit: Int?,
    offset: Long?
): Either<E, List<Article>> =
    byQuery(
        query = query
            .andWhere { ArticlesTable.archiveDate less DateTime.now() }
            .orderBy(ArticlesTable.archiveDate to SortOrder.DESC),
        limit = limit,
        offset = offset
    )


private suspend fun <V, E> Service<V, E, Article>.updateArticle(
    id: Int,
    statement: UpdateStatement.() -> Unit
): Either<E, Id<Article>> =
    Either.catch {
        transactionEffect(ArticlesTable) {
            update({ ArticlesTable.id eq id }) { it.run(statement) }
        }.id<Article>()
    }.mapLeft { errorHandler.handle(it) }


/**
 * The returned ArticleIndices refer to the parent articles which were selected by this method.
 */
suspend fun <V, E> Service<V, E, Article>.createRecurrentArticles(): Either<E, List<Id<Article>>> {

    /**
     * Selects all articles for which a recurrent child can currently be created
     */
    suspend fun selectRecurrentArticles(): Either<E, List<Pair<Article, Article>>> = either {
        val articles = !byQuery(
            query = ArticlesTable.select {
                (ArticlesTable.isRecurrent eq true) and
                        (ArticlesTable.applicationDeadline lessEq DateTime.now()) and
                        ArticlesTable.childArticle.isNull()
            }
        )

        articles.map { it to it.recurrentCopy() }
    }


    /**
     * Creates an action [F] which updates the parent article and creates the new child article.
     * Error handling happens in two ways:
     *      1. If any of the functions within the concurrent block throw an error, the whole operation will
     *         short circuit.
     *      2. If the child article for some unknown reason cannot be validated, the method will throw an error
     *         which contains details as to what validation errors caused the failure.
     */
    suspend fun runRecurrenceUpdate(parent: Article, child: Article): Either<E, Id<Article>> =
        either {
            val parentKey = parent.id?.id!!
            val childKey = child.id?.id!!

            !when (val res = validator.validate(child)) {
                is Validated.Valid -> create(res.a)
                is Validated.Invalid -> errorHandler.validationFailed(res.e).left()
            }

            !updateArticle(parentKey) {
                this[ArticlesTable.childArticle] = childKey
                this[ArticlesTable.isRecurrent] = false
            }

        }


    return either {
        val recurrentArticles = !selectRecurrentArticles()

        recurrentArticles.parTraverse { (parent, child) -> !runRecurrenceUpdate(parent, child) }
    }
}


internal fun Query.paginate(limit: Int?, offset: Long?): Query = apply {
    if (limit != null) {
        if (offset != null) {
            limit(limit, offset)
        }
        limit(limit)
    }
}

