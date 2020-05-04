package model.extensions

import model.Article
import model.Coerce
import org.jetbrains.exposed.sql.ResultRow
import repository.ContactRepository
import repository.dao.ArticlesTable
import repository.keyOf
import repository.readRecurrence


val Article.Companion.resultRowCoerce
    get() = object : Coerce<ResultRow, Article> {
        override suspend fun ResultRow.coerce(): Article =
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
                contactPartner = this[ArticlesTable.contactPartner]?.let { ContactRepository.byId(keyOf(it)) },
                childArticle = this[ArticlesTable.childArticle]?.let { keyOf<Article>(it) },
                parentArticle = this[ArticlesTable.parentArticle]?.let { keyOf<Article>(it) }
            )

    }