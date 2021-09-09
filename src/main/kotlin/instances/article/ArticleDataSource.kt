package instances.article

import arrow.core.computations.nullable
import model.Article
import model.RecurrentInfo
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update
import types.*
import types.dao.ArticlesTable

private val ArticleDeserializer = FromDB {
    Article(
        id = this[ArticlesTable.id].value.let(::Id),
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
        contactPartner = this[ArticlesTable.contactPartner]?.let(::Id),
        childArticle = this[ArticlesTable.childArticle]?.let(::Id),
        parentArticle = this[ArticlesTable.parentArticle]?.let(::Id)
    )
}

private val ArticleSerializer = IntoDB<Article> { builder ->
    builder.also { stm ->
        stm[ArticlesTable.title] = title
        stm[ArticlesTable.text] = text
        stm[ArticlesTable.rubric] = rubric
        stm[ArticlesTable.priority] = priority
        stm[ArticlesTable.targetGroup] = targetGroup
        stm[ArticlesTable.supportType] = supportType
        stm[ArticlesTable.subject] = subject
        stm[ArticlesTable.state] = state
        stm[ArticlesTable.archiveDate] = archiveDate
        stm[ArticlesTable.applicationDeadline] = applicationDeadline
        stm[ArticlesTable.contactPartner] = contactPartner?.identifier
        stm[ArticlesTable.childArticle] = childArticle?.identifier
        stm[ArticlesTable.parentArticle] = parentArticle?.identifier
        stm[ArticlesTable.isRecurrent] = recurrentInfo != null
        stm[ArticlesTable.recurrentCheckFrom] = recurrentInfo?.recurrentCheckFrom
        stm[ArticlesTable.nextApplicationDeadline] = recurrentInfo?.applicationDeadline
        stm[ArticlesTable.nextArchiveDate] = recurrentInfo?.archiveDate
    }
}


internal fun readRecurrence(resultRow: ResultRow): RecurrentInfo? = nullable.eager {
    val checkFrom = resultRow[ArticlesTable.recurrentCheckFrom].bind()
    val deadline = resultRow[ArticlesTable.nextApplicationDeadline].bind()
    val archiveDate = resultRow[ArticlesTable.nextArchiveDate].bind()

    RecurrentInfo(checkFrom, deadline, archiveDate)
}


object ArticleReader : ReadDB<Article>, FromDB<Article> by ArticleDeserializer {
    override val context: DatabaseContext = DatabaseContext(ArticlesTable)
}

object ArticleWriter : WriteDB<Article>, IntoDB<Article> by ArticleSerializer {
    override val context: DatabaseContext = DatabaseContext(ArticlesTable)

    override suspend fun delete(entity: Id<Article>): Unit = transactionEffect(ArticlesTable) {
        update({ childArticle eq entity.identifier }) { it[childArticle] = null }
        update({ parentArticle eq entity.identifier }) { it[parentArticle] = null }
        deleteWhere { id eq entity.identifier }
    }

}