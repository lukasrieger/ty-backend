package repository.extensions

import arrow.Kind
import arrow.core.Validated
import arrow.fx.typeclasses.Concurrent
import arrow.typeclasses.Show
import model.Article
import model.recurrentCopy
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.joda.time.DateTime
import repository.*
import repository.dao.ArticlesTable


internal fun <F> Concurrent<F>.queryPaginate(
    query: Query,
    limit: Int? = null,
    offset: Long? = null,
    ordering: () -> Pair<Column<DateTime>, SortOrder> = { ArticlesTable.applicationDeadline to SortOrder.ASC }
): Kind<F, Query> = fx.concurrent {
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
fun <F> Reader<F, Article>.byQueryArchived(
    query: Query,
    limit: Int?,
    offset: Long?
): Kind<F, QueryResult<Article>> =
    byQuery(
        query = query
            .andWhere { ArticlesTable.archiveDate less DateTime.now() }
            .orderBy(ArticlesTable.archiveDate to SortOrder.DESC),
        limit = limit,
        offset = offset

    )


private fun <F> Writer<F, Article>.updateArticle(
    id: Int,
    statement: UpdateStatement.() -> Unit
): Kind<F, ArticleIndex> =
    runtime.fx.concurrent {
        val dbResult = !transactionEffect(ArticlesTable) {
            update({ ArticlesTable.id eq id }) { it.run(statement) }
        }
        keyOf<Article>(dbResult)
    }


private fun <F> Repository<F, Article>.selectRecurrentArticles(): Kind<F, List<Pair<Article, Article>>> =
    runtime.fx.concurrent {
        byQuery(
            query = ArticlesTable.select {
                (ArticlesTable.isRecurrent eq true) and
                        (ArticlesTable.applicationDeadline lessEq DateTime.now()) and
                        ArticlesTable.childArticle.isNull()
            }
        ).bind().result.map { it to it.recurrentCopy() }
    }

private fun <F> Repository<F, Article>.runRecurrenceUpdate(parent: Article, child: Article): Kind<F, ArticleIndex> =
    runtime.fx.concurrent {
        val (parentKey) = parent.id
        val (childKey) = child.id

        val x = !when (val res = !validator.validate(child)) {
            is Validated.Valid -> create(res)
            is Validated.Invalid -> raiseError(Throwable(res.e.show(Show.any())))
        }

        !updateArticle(parentKey) {
            this[ArticlesTable.childArticle] = childKey
            this[ArticlesTable.isRecurrent] = false
        }

    }

fun <F> Repository<F, Article>.createRecurrentArticles(): Kind<F, List<ArticleIndex>> =
    runtime.fx.concurrent {
        val recurrentArticles = !selectRecurrentArticles()

        recurrentArticles.map { (parent, child) -> !runRecurrenceUpdate(parent, child) }
    }


internal fun Query.paginate(limit: Int?, offset: Long?): Query = apply {
    if (limit != null) {
        if (offset != null) {
            limit(limit, offset)
        }
        limit(limit)
    }
}


