package repository

import arrow.core.None
import arrow.core.Option
import arrow.core.extensions.option.applicative.applicative
import arrow.core.fix
import arrow.core.toOption
import kotlinx.coroutines.Dispatchers
import model.Article
import model.RecurrentInfo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.dsl.module
import repository.ContactRepository.byId
import repository.dao.ArticlesTable
import repository.extensions.queryResultSet


val articleModule = module {

    single<ReadableRepository<Article>> { ArticleReader }
    single<WritableRepository<Article>> { ArticleWriter }
    single<Repository<Article>> { ArticleRepository }
}

internal typealias ArticleIndex = PrimaryKey<Article>


object ArticleReader : ReadableRepository<Article> {

    override suspend fun byId(id: PrimaryKey<Article>): Option<Article> =
        newSuspendedTransaction(Dispatchers.IO) {
            ArticlesTable.select { ArticlesTable.id eq id.key }
                .orderBy(ArticlesTable.applicationDeadline to SortOrder.ASC)
                .singleOrNull()
                .asOption(ResultRow::toArticle)
        }

    override suspend fun countOf(query: Query): Int =
        newSuspendedTransaction(Dispatchers.IO) {
            query.count()
        }


    override suspend fun byQuery(query: Query, limit: Int?, offset: Int?): QueryResult<Article> =
        (ArticleRepository.countOf(query) to queryResultSet(query, limit, offset)
            .map { it.toArticle() }
                ).let { (count, seq) -> QueryResult(count, seq) }

}


object ArticleWriter : WritableRepository<Article> {

    override suspend fun update(entry: Article): Result<ArticleIndex> =
        newSuspendedTransaction(Dispatchers.IO) {
            ArticlesTable.runCatching {
                val (key) = entry.id
                update({ ArticlesTable.id eq key }) { entry.toStatement(it) }
            }.mapCatching {
                keyOf<Article>(it)
            }
        }.foldEither()


    override suspend fun create(entry: Article): Result<Article> =
        newSuspendedTransaction(Dispatchers.IO) {
            ArticlesTable.runCatching {
                insert {
                    entry.toStatement(it)
                } get ArticlesTable.id
            }.mapCatching {
                entry.copy(id = keyOf(it.value))
            }
        }.foldEither()


    override suspend fun delete(id: PrimaryKey<Article>): Result<ArticleIndex> =
        newSuspendedTransaction(Dispatchers.IO) {
            ArticlesTable.runCatching {

                // make sure that no references to the deleted entry are left in the table
                update({ childArticle eq id.key }) {
                    it[childArticle] = null
                }
                update({ parentArticle eq id.key }) {
                    it[parentArticle] = null
                }

                deleteWhere { ArticlesTable.id eq id.key }

            }.mapCatching {
                keyOf<Article>(it)
            }
        }.foldEither()


}


object ArticleRepository :
    ReadableRepository<Article> by ArticleReader,
    WritableRepository<Article> by ArticleWriter,
    Repository<Article>


internal fun readRecurrence(row: ResultRow): Option<RecurrentInfo> =
    Option.applicative()
        .map(
            row[ArticlesTable.recurrentCheckFrom].toOption(),
            row[ArticlesTable.nextApplicationDeadline].toOption(),
            row[ArticlesTable.nextArchiveDate].toOption()
        ) { (rec, app, arch) ->
            RecurrentInfo(rec, app, arch)
        }.fix()


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


private suspend fun <T> fromNullable(
    id: Int?,
    res: suspend (PrimaryKey<T>) -> Option<T>
): Option<T> = if (id != null) res(keyOf(id)) else None
