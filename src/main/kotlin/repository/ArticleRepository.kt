package repository

import arrow.core.None
import arrow.core.Option
import arrow.core.Valid
import arrow.core.extensions.fx
import arrow.core.toOption
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

    override suspend fun byId(id: PrimaryKey<Article>): Option<Article> =
        newSuspendedTransaction(Dispatchers.IO) {
            ArticlesTable.select { ArticlesTable.id eq id.key }
                .orderBy(ArticlesTable.applicationDeadline to SortOrder.ASC)
                .singleOrNull()
                .asOption(ResultRow::toArticle)
        }

    override suspend fun countOf(query: Query): Long = newSuspendedTransaction(Dispatchers.IO) { query.count() }

    override suspend fun byQuery(query: Query, limit: Int?, offset: Long?): QueryResult<Article> {
        val pagedQuery = queryPaginate(query, limit, offset).map { it.toArticle() }
        return QueryResult(countOf(query), pagedQuery)
    }

}

object ArticleWriter : Writer<Article> {

    override suspend fun update(entry: ValidArticle): Result<ArticleIndex> = safeTransactionIO(ArticlesTable) {
        val (article) = entry
        val (key) = article.id
        update({ ArticlesTable.id eq key }) { article.toStatement(it) }
    }.map { keyOf<Article>(it) }


    override suspend fun create(entry: ValidArticle): Result<Article> = safeTransactionIO(ArticlesTable) {
        val (article) = entry
        insert { article.toStatement(it) } get id
    }.map { Article.id.set(entry.a, keyOf(it.value)) }


    override suspend fun delete(id: PrimaryKey<Article>): Result<ArticleIndex> = safeTransactionIO(ArticlesTable) {
        update({ childArticle eq id.key }) { it[childArticle] = null }
        update({ parentArticle eq id.key }) { it[parentArticle] = null }
        deleteWhere { ArticlesTable.id eq id.key }
    }.map { keyOf<Article>(it) }
}


object ArticleRepository :
    Reader<Article> by ArticleReader,
    Writer<Article> by ArticleWriter,
    Repository<Article>


internal fun readRecurrence(row: ResultRow): Option<RecurrentInfo> = Option.fx {
    val rec = !row[ArticlesTable.recurrentCheckFrom].toOption()
    val app = !row[ArticlesTable.nextApplicationDeadline].toOption()
    val arch = !row[ArticlesTable.nextArchiveDate].toOption()

    RecurrentInfo(rec, app, arch)
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
        contactPartner = fromNullable(this[ArticlesTable.contactPartner]) { byId(it) },
        childArticle = this[ArticlesTable.childArticle].toOption().map { keyOf<Article>(it) },
        parentArticle = this[ArticlesTable.parentArticle].toOption().map { keyOf<Article>(it) }
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
        this[ArticlesTable.contactPartner] = contactPartner.map { it.id.key }.orNull()
        this[ArticlesTable.childArticle] = childArticle.map { it.key }.orNull()
        this[ArticlesTable.parentArticle] = parentArticle.map { it.key }.orNull()
        this[ArticlesTable.isRecurrent] = recurrentInfo.isDefined()
        this[ArticlesTable.recurrentCheckFrom] = recurrentInfo.map { it.recurrentCheckFrom }.orNull()
        this[ArticlesTable.nextApplicationDeadline] = recurrentInfo.map { it.applicationDeadline }.orNull()
        this[ArticlesTable.nextArchiveDate] = recurrentInfo.map { it.archiveDate }.orNull()
    }


private suspend fun <T> fromNullable(id: Int?, res: suspend (PrimaryKey<T>) -> Option<T>): Option<T> =
    if (id != null) res(keyOf(id)) else None
