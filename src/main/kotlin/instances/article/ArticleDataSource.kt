package instances.article

import arrow.fx.coroutines.parTraverse
import model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import service.*
import service.dao.ArticlesTable


private fun Article.toStatement(statement: UpdateBuilder<Int>): Unit = statement.run {
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
    this[ArticlesTable.contactPartner] = contactPartner?.id?.id
    this[ArticlesTable.childArticle] = childArticle?.id
    this[ArticlesTable.parentArticle] = parentArticle?.id
    this[ArticlesTable.isRecurrent] = recurrentInfo != null
    this[ArticlesTable.recurrentCheckFrom] = recurrentInfo?.recurrentCheckFrom
    this[ArticlesTable.nextApplicationDeadline] = recurrentInfo?.applicationDeadline
    this[ArticlesTable.nextArchiveDate] = recurrentInfo?.archiveDate
}


internal class ArticleDataSource(private val contactReader: Reader<*, *, ContactPartner>) : DataSource<Article> {

    override suspend fun get(id: Id<Article>): Article? =
        transactionEffect(ArticlesTable) {
            select { ArticlesTable.id eq id.id }
                .singleOrNull()
                ?.toArticle()
        }

    override suspend fun get(query: Query, limit: Int?, offset: Long?): List<Article> =
        transactionEffect(ArticlesTable) {
            query.parTraverse { it.toArticle() }
        }

    override suspend fun count(query: Query): Long =
        transactionEffect(ArticlesTable) { query.count() }

    override suspend fun update(value: Article) =
        transactionEffect(ArticlesTable) {
            update({ id eq value.id?.id }) { value.toStatement(it) }
            Unit
        }

    override suspend fun create(value: Article): Article =
        transactionEffect(ArticlesTable) {
            val id = insert { value.toStatement(it) } get id
            value.copy(id = id.value.id())
        }

    override suspend fun delete(id: Id<Article>) =
        transactionEffect(ArticlesTable) {
            update({ childArticle eq id.id }) { it[childArticle] = null }
            update({ parentArticle eq id.id }) { it[parentArticle] = null }
            deleteWhere { ArticlesTable.id eq id.id }
            Unit
        }


    private suspend fun ResultRow.toArticle(): Article {
        val contactPartner =
            this@toArticle[ArticlesTable.contactPartner]?.let {
                contactReader.dataSource.get(it.id())
            }

        return Article(
            id = this@toArticle[ArticlesTable.id].value.id(),
            title = this@toArticle[ArticlesTable.title],
            text = this@toArticle[ArticlesTable.text],
            rubric = this@toArticle[ArticlesTable.rubric],
            priority = this@toArticle[ArticlesTable.priority],
            targetGroup = this@toArticle[ArticlesTable.targetGroup],
            supportType = this@toArticle[ArticlesTable.supportType],
            subject = this@toArticle[ArticlesTable.subject],
            state = this@toArticle[ArticlesTable.state],
            archiveDate = this@toArticle[ArticlesTable.archiveDate],
            recurrentInfo = readRecurrence(this@toArticle),
            applicationDeadline = this@toArticle[ArticlesTable.applicationDeadline],
            contactPartner = contactPartner,
            childArticle = this@toArticle[ArticlesTable.childArticle]?.id(),
            parentArticle = this@toArticle[ArticlesTable.parentArticle]?.id()
        )
    }

}


fun readRecurrence(resultRow: ResultRow) =
    resultRow[ArticlesTable.recurrentCheckFrom]?.let { checkFrom ->
        resultRow[ArticlesTable.nextApplicationDeadline]?.let { deadline ->
            resultRow[ArticlesTable.nextArchiveDate]?.let { archiveDate ->
                RecurrentInfo(
                    recurrentCheckFrom = checkFrom,
                    applicationDeadline = deadline,
                    archiveDate = archiveDate
                )
            }
        }
    }