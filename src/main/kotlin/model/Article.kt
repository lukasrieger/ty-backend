package model

import arrow.core.Option
import org.joda.time.DateTime
import repository.None
import repository.PrimaryKey

data class Article(
    val id: PrimaryKey<Article> = None,
    val name: String,
    val text: String,
    val rubric: Rubric,
    val priority: Int,
    val targetGroup: TargetGroup,
    val supportType: SupportType,
    val state: ArticleState,
    val archiveDate: DateTime,
    val isRecurrent: Boolean,
    val applicationDeadline: DateTime,
    val contactPartner: Option<ContactPartner>,
    val childArticle: Option<Article>,
    val parentArticle: Option<Article>
) {
    val hasChild
        get() = !childArticle.isEmpty()

    val hasParent
        get() = !parentArticle.isEmpty()


}

fun Article.recurrentCopy() = copy()

