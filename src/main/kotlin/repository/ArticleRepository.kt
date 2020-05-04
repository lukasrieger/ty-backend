package repository

import arrow.core.Either
import arrow.core.Valid
import kotlinx.coroutines.Dispatchers
import model.Article
import model.RecurrentInfo
import model.id
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.dsl.module
import repository.ContactRepository.byId
import repository.dao.ArticlesTable
import repository.extensions.queryPaginate


val articleModule = module {

    single { ArticleReader }
    single { ArticleWriter }
    single { ArticleRepository }
}

typealias ArticleIndex = PrimaryKey<Article>
typealias ValidArticle = Valid<Article>

object ArticleReader : Reader<Article> {

    override suspend fun byId(id: PrimaryKey<Article>): Article? =
        newSuspendedTransaction(Dispatchers.IO) {
            ArticlesTable.select { ArticlesTable.id eq id.key }
                .orderBy(ArticlesTable.applicationDeadline to SortOrder.ASC)
                .singleOrNull()
                ?.toArticle()
        }

    override suspend fun countOf(query: Query): Long = newSuspendedTransaction(Dispatchers.IO) { query.count() }

    override suspend fun byQuery(query: Query, limit: Int?, offset: Long?): QueryResult<Article> {
        val pagedQuery = queryPaginate(query, limit, offset).map { it.toArticle() }
        return QueryResult(countOf(query), pagedQuery)
    }

}

object ArticleWriter : Writer<Article> {

    override suspend fun update(entry: ValidArticle): Either<Throwable, ValidArticle> =
        safeTransactionIO(ArticlesTable) {
            val (article) = entry
            val (key) = article.id
            update({ ArticlesTable.id eq key }) { article.toStatement(it) }
        }.map { entry }


    override suspend fun create(entry: ValidArticle): Either<Throwable, ValidArticle> =
        safeTransactionIO(ArticlesTable) {
            val (article) = entry
            insert { article.toStatement(it) } get id
        }.map { Valid(Article.id.set(entry.a, keyOf(it.value))) }


    override suspend fun delete(id: PrimaryKey<Article>): Either<Throwable, ArticleIndex> =
        safeTransactionIO(ArticlesTable) {
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


internal suspend inline fun ResultRow.toArticle(): Article =
    Article(
        id = keyOf(this[ArticlesTable.id].value),
        title = this[ArticlesTable.title],
        text = this[ArticlesTable.text],
        rubric = this[ArticlesTable.rubric],
        priority = this[ArticlesTable.priority],
        targetGroup = this[ArticlesTable.targetGroup],
        supportType = this[ArticlesTable.supportType],
        subject = this[ArticlesTable.subject],
        state = this[ArticlesTable.state],
        archiveDate = this[ArticlesTable.archiveDate],
        recurrentInfo = readRecurrence(this),
        applicationDeadline = this[ArticlesTable.applicationDeadline],
        contactPartner = this[ArticlesTable.contactPartner]?.let { byId(keyOf(it)) },
        childArticle = this[ArticlesTable.childArticle]?.let { keyOf<Article>(it) },
        parentArticle = this[ArticlesTable.parentArticle]?.let { keyOf<Article>(it) }
    )


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
