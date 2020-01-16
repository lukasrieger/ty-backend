package repository.dao

import model.ArticleState
import model.Rubric
import model.SupportType
import model.TargetGroup
import org.jetbrains.exposed.dao.IntIdTable


object ArticlesTable : IntIdTable() {
    val name = text("name")
    val text = text("text")
    val rubric = enumerationByName("rubric", 50, Rubric::class)
    val priority = integer("priority")
    val targetGroup = enumerationByName("targetGroup", 50, TargetGroup::class)
    val supportType = enumerationByName("supportType", 50, SupportType::class)
    val state = enumerationByName("state", 50, ArticleState::class)
    val archiveDate = date("archiveDate")
    val isRecurrent = bool("isRecurrent")
    val applicationDeadline = date("applicationDeadline")
    val contactPartner = integer("contactPartner").references(ContactTable.id).nullable()
    val childArticle = integer("childArticle").references(id).nullable()
    val parentArticle = integer("parentArticle").references(id).nullable()
}