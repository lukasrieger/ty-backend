package repository.dao

import model.*
import org.jetbrains.exposed.dao.IntIdTable


/**
 * Table definition with rows that correspond to the properties of the [model.Article] type.
 */
object ArticlesTable : IntIdTable() {
    val title = text("title")
    val text = text("text")
    val rubric = enumerationByName("rubric", 50, Rubric::class)
    val priority = integer("priority")
    val targetGroup = enumerationByName("targetGroup", 50, TargetGroup::class)
    val supportType = enumerationByName("supportType", 50, SupportType::class)
    val subject = enumerationByName("subject", 50, Subject::class)
    val state = enumerationByName("state", 50, ArticleState::class)
    val archiveDate = date("archiveDate")
    val applicationDeadline = date("applicationDeadline")
    val contactPartner = integer("contactPartner").references(ContactTable.id).nullable()
    val childArticle = integer("childArticle").references(id).nullable()
    val parentArticle = integer("parentArticle").references(id).nullable()

    val isRecurrent = bool("isRecurrent")
    val recurrentCheckFrom = date("recurrentCheckFrom").nullable()
    val nextApplicationDeadline = date("nextApplicationDeadline").nullable()
    val nextArchiveDate = date("archiveDate").nullable()
}