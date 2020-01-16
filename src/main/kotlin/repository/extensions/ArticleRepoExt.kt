package repository.extensions

import kotlinx.coroutines.Dispatchers
import model.Article
import model.recurrentCopy
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.joda.time.DateTime
import repository.*
import repository.dao.ArticlesTable
import repository.toArticle


internal suspend fun queryResultSet(
    query: Query,
    limit: Int? = null,
    offset: Int? = null,
    ordering: Ordering<DateTime, SortOrder> = Ordering(ArticlesTable.applicationDeadline to SortOrder.ASC)
) =
    newSuspendedTransaction(Dispatchers.IO) {
        val (ord) = ordering
        query
            .also { query ->
                limit?.let {
                    offset?.let { query.limit(limit, offset) }
                    query.limit(limit)
                }
            }
            .orderBy(ord)
    }

suspend fun Repository<Article>.byQueryArchived(limit: Int?, offset: Int?, query: Query): QueryResult<Article> =
    (countOf(query) to queryResultSet(query, limit, offset, orderOf {
        ArticlesTable.archiveDate to SortOrder.DESC
    })
        .andWhere { ArticlesTable.archiveDate less DateTime.now() }
        .map { it.toArticle() }
            ).let { (count, seq) -> QueryResult(count, seq) }


private suspend fun updateArticle(id: Int, statement: UpdateStatement.() -> Unit) =
    newSuspendedTransaction(Dispatchers.IO) {
        ArticlesTable.update({ ArticlesTable.id eq id }) {
            it.run(statement)
        }
    }

suspend fun Repository<Article>.createRecurrentArticles() =
    newSuspendedTransaction(Dispatchers.IO) {
        ArticlesTable.select {

            (ArticlesTable.isRecurrent eq true) and
                    (ArticlesTable.applicationDeadline lessEq DateTime.now()) and
                    ArticlesTable.childArticle.isNull()

        }
            .map { it.toArticle().let { article -> article to article.recurrentCopy() } }
    }

        .forEach {
            // create recurrent article in the database and update old article with ID of new child
            val (parent, child) = it

            val (childKey) = ArticleRepository.create(child)
            val (parentKey) = parent.id

            updateArticle(parentKey) {
                this[ArticlesTable.childArticle] = childKey
            }
        }
