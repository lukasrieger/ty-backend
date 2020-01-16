package repository

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import model.Article
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import repository.ArticleRepository.byId
import repository.ContactRepository.byId
import repository.dao.ArticlesTable
import repository.extensions.queryResultSet
import kotlin.coroutines.CoroutineContext


object ArticleRepository : Repository<Article>, CoroutineScope {


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + CoroutineName("ArticleRepository")

    init {
        transaction {
            SchemaUtils.create(ArticlesTable)
        }

    }


    override suspend fun byId(id: PrimaryKey<Article>): Option<Article> =
        newSuspendedTransaction(Dispatchers.IO) {
            ArticlesTable.select { ArticlesTable.id eq id.key }
                .orderBy(ArticlesTable.applicationDeadline to SortOrder.ASC)
                .singleOrNull()
        }.asOption()


    override suspend fun byQuery(query: Query, limit: Int?, offset: Int?): QueryResult<Article> =
        (countOf(query) to queryResultSet(query, limit, offset)
            .map { it.toArticle() }
                ).let { (count, seq) -> QueryResult(count, seq) }


    override suspend fun update(entry: Article): PrimaryKey<Article> =
        newSuspendedTransaction(Dispatchers.IO) {
            val (key) = entry.id
            ArticlesTable.update({ ArticlesTable.id eq key }) { entry.toStatement(it) }
        }.let(::keyOf)


    override suspend fun create(entry: Article): PrimaryKey<Article> =
        newSuspendedTransaction(Dispatchers.IO) {
            ArticlesTable.insert {
                entry.toStatement(it)
            } get ArticlesTable.id
        }.value.let(::keyOf)


    override suspend fun countOf(query: Query): Int =
        newSuspendedTransaction(Dispatchers.IO) {
            query.count()
        }


    override suspend fun delete(id: PrimaryKey<Article>): PrimaryKey<Article> =
        newSuspendedTransaction(Dispatchers.IO) {
            ArticlesTable.deleteWhere { ArticlesTable.id eq id.key }
        }.let(::keyOf)

}


internal suspend fun ResultRow.toArticle(): Article =
    Article(
        id = keyOf(this[ArticlesTable.id].value),
        name = this[ArticlesTable.name],
        text = this[ArticlesTable.text],
        rubric = this[ArticlesTable.rubric],
        priority = this[ArticlesTable.priority],
        targetGroup = this[ArticlesTable.targetGroup],
        supportType = this[ArticlesTable.supportType],
        state = this[ArticlesTable.state],
        archiveDate = this[ArticlesTable.archiveDate],
        isRecurrent = this[ArticlesTable.isRecurrent],
        applicationDeadline = this[ArticlesTable.applicationDeadline],
        contactPartner = fromNullable(
            this[ArticlesTable.contactPartner]
        ) { byId(it) },
        childArticle = fromNullable(this[ArticlesTable.childArticle]) { byId(it) },
        parentArticle = fromNullable(this[ArticlesTable.parentArticle]) { byId(it) }

    )


private fun Article.toStatement(statement: UpdateBuilder<Int>) =
    statement.run {
        this[ArticlesTable.name] = name
        this[ArticlesTable.text] = text
        this[ArticlesTable.rubric] = rubric
        this[ArticlesTable.priority] = priority
        this[ArticlesTable.targetGroup] = targetGroup
        this[ArticlesTable.supportType] = supportType
        this[ArticlesTable.state] = state
        this[ArticlesTable.archiveDate] = archiveDate
        this[ArticlesTable.isRecurrent] = isRecurrent
        this[ArticlesTable.applicationDeadline] = applicationDeadline
        this[ArticlesTable.contactPartner] = contactPartner.map { it.id }.orNull()
        this[ArticlesTable.childArticle] = childArticle.map { it.id.key }.orNull()
        this[ArticlesTable.parentArticle] = parentArticle.map { it.id.key }.orNull()
    }


private suspend fun ResultRow?.asOption(): Option<Article> = when (this) {
    null -> None
    else -> Some(this.toArticle())
}


private suspend fun <T> fromNullable(
    id: Int?,
    res: suspend (PrimaryKey<T>) -> Option<T>
): Option<T> = id?.let { res(keyOf(it)) } ?: None
