package types.dao

import model.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.jodatime.date


object ArticlesTable : IntIdTable() {
    val title = text("title")
    val text = text("text")
    val rubric = enumerationByName("rubric", 50, Rubric::class)
    val priority = enumerationByName("priority", 50, Priority::class)
    val targetGroup = enumerationByName("targetGroup", 50, TargetGroup::class)
    val supportType = enumerationByName("supportType", 50, SupportType::class)
    val subject = enumerationByName("subject", 50, Subject::class)
    val state = enumerationByName("state", 50, ArticleState::class)
    val archiveDate = date("archiveDate")
    val applicationDeadline = date("applicationDeadline")
    val contactPartner = integer("contactPartner").nullable()
    val childArticle = integer("childArticle").nullable()
    val parentArticle = integer("parentArticle").nullable()

    val isRecurrent = bool("isRecurrent")
    val recurrentCheckFrom = date("recurrentCheckFrom").nullable()
    val nextApplicationDeadline = date("nextApplicationDeadline").nullable()
    val nextArchiveDate = date("nextArchiveDate").nullable()

}