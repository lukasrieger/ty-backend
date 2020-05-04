package repository

import arrow.core.Either
import arrow.core.Valid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import model.Article
import model.RecurrentInfo
import model.Source
import model.extensions.resultRowCoerce
import model.id
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.dsl.module
import repository.dao.ArticlesTable
import repository.extensions.queryPaginate
import java.io.FileInputStream


val articleModule = module {

    single { ArticleReader }
    single { ArticleWriter }
    single { ArticleRepository }
}

typealias ArticleIndex = PrimaryKey<Article>
typealias ValidArticle = Valid<Article>

object ArticleReader : Reader<Article> {

    private val context = Dispatchers.IO

    override suspend fun byId(id: PrimaryKey<Article>): Article? = with(Article.resultRowCoerce) {
        newSuspendedTransaction(context) {
            ArticlesTable.select { ArticlesTable.id eq id.key }
                .orderBy(ArticlesTable.applicationDeadline to SortOrder.ASC)
                .singleOrNull()
                ?.coerce()
        }
    }

    override suspend fun countOf(query: Query): Long = newSuspendedTransaction(context) { query.count() }

    override suspend fun byQuery(query: Query, limit: Int?, offset: Long?): QueryResult<Article> =
        with(Article.resultRowCoerce) {
            val count = countOf(query)
            val pagedQuery = queryPaginate(query, limit, offset).map { it.coerce() }
            return QueryResult(count, pagedQuery)
        }


}

object ArticleWriter : Writer<Article> {

    override suspend fun update(entry: ValidArticle): Either<Throwable, ValidArticle> =
        transactionContext(ArticlesTable) {
            val (article) = entry
            val (key) = article.id
            update({ ArticlesTable.id eq key }) { article.toStatement(it) }
        }.map { entry }


    override suspend fun create(entry: ValidArticle): Either<Throwable, ValidArticle> =
        transactionContext(ArticlesTable) {
            val (article) = entry
            insert { article.toStatement(it) } get id
        }.map { Valid(Article.id.set(entry.a, keyOf(it.value))) }


    override suspend fun delete(id: PrimaryKey<Article>): Either<Throwable, ArticleIndex> =
        transactionContext(ArticlesTable) {
            update({ childArticle eq id.key }) { it[childArticle] = null }
            update({ parentArticle eq id.key }) { it[parentArticle] = null }
            deleteWhere { ArticlesTable.id eq id.key }
        }.map { keyOf<Article>(it) }
}


object ArticleRepository :
    Reader<Article> by ArticleReader,
    Writer<Article> by ArticleWriter,
    Repository<Article>


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
            val text = bytes.contentToString()

            Source(url, text)
        }
    }
}


