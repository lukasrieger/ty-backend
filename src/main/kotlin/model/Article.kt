package model

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.optics.optics
import org.joda.time.DateTime
import repository.PrimaryKey
import repository.None as NullKey


@optics data class Article(
    val id: PrimaryKey<Article> = NullKey,
    val title: String,
    val text: String,
    val rubric: Rubric,
    val priority: Int,
    val targetGroup: TargetGroup,
    val supportType: SupportType,
    val subject: Subject,
    val state: ArticleState,
    val archiveDate: DateTime,
    val recurrentInfo: Option<RecurrentInfo>,
    val applicationDeadline: DateTime,
    val contactPartner: Option<ContactPartner> = None,
    val childArticle: Option<PrimaryKey<Article>> = None,
    val parentArticle: Option<PrimaryKey<Article>> = None
) {
    val hasChild
        get() = childArticle.isDefined()

    val hasParent
        get() = !parentArticle.isDefined()

    val isRecurrent
        get() = recurrentInfo.isDefined()
}


/**
 * This function creates a child article for the article.
 * The new article receives the current one as a reference in its parentArticle field.
 * @receiver Article
 * @return Article
 */
fun Article.recurrentCopy() = copy(
    id = NullKey,
    parentArticle = Some(id),
    childArticle = None
)


data class RecurrentInfo(
    val recurrentCheckFrom: DateTime,
    val applicationDeadline: DateTime,
    val archiveDate: DateTime
)