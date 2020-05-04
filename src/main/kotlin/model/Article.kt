package model

import arrow.optics.optics
import org.joda.time.DateTime
import repository.PrimaryKey
import repository.Init as NullKey

@optics
data class Article(
    val id: PrimaryKey<Article> = NullKey,
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
    val childArticle: PrimaryKey<Article>? = null,
    val parentArticle: PrimaryKey<Article>? = null,
    val attachedSource: Source? = null
) {
    companion object

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
fun Article.recurrentCopy() = copy(
    id = NullKey,
    parentArticle = id,
    childArticle = null
)


data class RecurrentInfo(
    val recurrentCheckFrom: DateTime,
    val applicationDeadline: DateTime,
    val archiveDate: DateTime
)