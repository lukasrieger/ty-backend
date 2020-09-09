package model

import org.joda.time.DateTime
import service.Id
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

interface WithId {
    val idOf: Id<Article>
}

data class Article(
     val id: Id<Article>? = null,
     val title: String,
     val text: String,
     val rubric: Rubric,
     val priority: Priority,
     val targetGroup: TargetGroup,
     val supportType: SupportType,
     val subject: Subject,
     val state: ArticleState,
     val archiveDate: DateTime,
     val recurrentInfo: RecurrentInfo? = null,
     val applicationDeadline: DateTime,
     val contactPartner: ContactPartner? = null,
     val childArticle: Id<Article>? = null,
     val parentArticle: Id<Article>? = null,
     val attachedSource: Source? = null
) : WithId {

    override val idOf = id!!

    val hasChild
        get() = childArticle != null

    val hasParent
        get() = parentArticle != null

    val isRecurrent
        get() = recurrentInfo != null
}


/**
 * This function creates a child article for the article.
 * The new article receives the current one as a reference in its parentArticle field.
 * @receiver Article
 * @return Article
 */
fun Article.recurrentCopy() =
     copy(
        id = null,
        parentArticle = id,
        childArticle = null
    )


data class RecurrentInfo(
    val recurrentCheckFrom: DateTime,
    val applicationDeadline: DateTime,
    val archiveDate: DateTime
)