package repository

import arrow.Kind
import arrow.core.Either
import arrow.core.Valid
import arrow.fx.typeclasses.Concurrent
import arrow.typeclasses.ApplicativeError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import model.Article
import model.RecurrentInfo
import model.Source
import model.extensions.fromResultRow
import model.id
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import repository.dao.ArticlesTable
import repository.extensions.queryPaginate
import java.io.FileInputStream


typealias ArticleIndex = PrimaryKey<Article>
typealias ValidArticle = Valid<Article>


class ArticleReader<F>(A: ApplicativeError<F, Throwable>) :
    Reader<F, Article>, ApplicativeError<F, Throwable> by A {

    @JvmName("nullableCoerce")
    private fun ResultRow?.coerce(): Kind<F, Article?> = TODO()
    private fun ResultRow.coerce(): Kind<F, Article> = TODO()


    override fun Concurrent<F>.byId(id: PrimaryKey<Article>): Kind<F, Article?> =
        fx.concurrent {
            !!transactionContext(ArticlesTable) {
                with(Article.fromResultRow) {
                    select { ArticlesTable.id eq id.key }
                        .orderBy(applicationDeadline to SortOrder.ASC)
                        .singleOrNull()
                        .coerce()
                }
            }
        }

    override fun Concurrent<F>.byQuery(query: Query, limit: Int?, offset: Long?): Kind<F, QueryResult<Article>> =
        fx.concurrent {
            val count = !countOf(query)
            val queryResult =
                queryPaginate(query, limit, offset).map { !it.coerce() }

            QueryResult(count, queryResult)
        }

    override fun Concurrent<F>.countOf(query: Query): Kind<F, Long> =
        fx.concurrent {
            !effect { newSuspendedTransaction { query.count() } }
        }

}


class ArticleWriter<F>(A: ApplicativeError<F, Throwable>) :
    Writer<F, Article>, ApplicativeError<F, Throwable> by A {


    override fun Concurrent<F>.update(entry: Valid<Article>): Kind<F, ValidArticle> =
        fx.concurrent {
            !transactionContext(ArticlesTable) {
                val (article) = entry
                val (key) = article.id
                ArticlesTable.update({ ArticlesTable.id eq key }) { article.toStatement(it) }
            }
            entry
        }

    override fun Concurrent<F>.create(entry: Valid<Article>): Kind<F, ValidArticle> =
        fx.concurrent {
            val id = !transactionContext(ArticlesTable) {
                val (article) = entry
                ArticlesTable.insert { article.toStatement(it) } get ArticlesTable.id
            }
            Valid(Article.id.set(entry.a, keyOf(id.value)))
        }

    override fun Concurrent<F>.delete(id: PrimaryKey<Article>): Kind<F, PrimaryKey<Article>> =
        fx.concurrent {
            !transactionContext(ArticlesTable) {
                update({ childArticle eq id.key }) { it[childArticle] = null }
                update({ parentArticle eq id.key }) { it[parentArticle] = null }
                deleteWhere { ArticlesTable.id eq id.key }
            }
            id
        }

}


class ArticleRepository<F>(A: ApplicativeError<F, Throwable>) :
    Reader<F, Article> by ArticleReader(A),
    Writer<F, Article> by ArticleWriter(A),
    Repository<F, Article>


internal fun readRecurrence(row: ResultRow): RecurrentInfo? =
    row[ArticlesTable.recurrentCheckFrom]?.let { rec ->
        row[ArticlesTable.nextApplicationDeadline]?.let { app ->
            row[ArticlesTable.nextArchiveDate]?.let { arch ->
                RecurrentInfo(rec, app, arch)
            }
        }
    }

private fun Article.toStatement(statement: UpdateBuilder<Int>) =
    statement.run {
        this[ArticlesTable.title] = title
        this[ArticlesTable.text] = text
        this[ArticlesTable.rubric] = rubric
        this[ArticlesTable.priority] = priority
        this[ArticlesTable.targetGroup] = targetGroup
        this[ArticlesTable.supportType] = supportType
        this[ArticlesTable.subject] = subject
        this[ArticlesTable.state] = state
        this[ArticlesTable.archiveDate] = archiveDate
        this[ArticlesTable.applicationDeadline] = applicationDeadline
        this[ArticlesTable.contactPartner] = contactPartner?.id?.key
        this[ArticlesTable.childArticle] = childArticle?.key
        this[ArticlesTable.parentArticle] = parentArticle?.key
        this[ArticlesTable.isRecurrent] = recurrentInfo != null
        this[ArticlesTable.recurrentCheckFrom] = recurrentInfo?.recurrentCheckFrom
        this[ArticlesTable.nextApplicationDeadline] = recurrentInfo?.applicationDeadline
        this[ArticlesTable.nextArchiveDate] = recurrentInfo?.archiveDate
    }

internal fun sourcePath(path: String): String = TODO()

internal suspend fun loadSource(url: String): Either<Throwable, Source> = Either.catch {
    withContext(Dispatchers.IO) {
        FileInputStream(sourcePath(url)).use {
            val bytes = it.readBytes()
            val text = String(bytes)

            Source(url, text)
        }
    }
}


