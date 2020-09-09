package instances.article

import arrow.core.invalid
import arrow.core.valid
import model.*
import org.joda.time.DateTime
import service.Id
import service.Reader
import validation.AbstractValidator
import validation.Validator


internal class DefaultArticleValidator(articleReader: Reader<*, *, Article>) : AbstractValidator<ArticleValidationError, Article>() {

    val validTitle = validation {
        if (it.title.isNotBlank()) {
            it.valid()
        } else {
            ArticleValidationError.BlankField(Article::title).invalid()
        }
    }

    val validApplicationDate = validation {
        if (it.applicationDeadline.isBefore(it.archiveDate)) {
            it.valid()
        } else {
            ArticleValidationError.InvalidApplicationDate(it.applicationDeadline).invalid()
        }
    }

    val validParentArticle = validation { article ->

        fun checkValidRelation(parent: Article, key: Id<Article>) =
            if (key == article.id) {
                article.valid()
            } else {
                ArticleValidationError.InvalidRelation(parent, article).invalid()
            }

        fun checkSymmetry(parent: Article) =
            article.childArticle?.let { checkValidRelation(parent, it) } ?: ArticleValidationError.AsymmetricRelation(
                parent,
                article
            ).invalid()


        suspend fun checkParentPresent(key: Id<Article>) =
            articleReader.byId(key)
                .fold(
                    ifLeft = { ArticleValidationError.MissingArticle(key).invalid() },
                    ifRight = { checkSymmetry(it) }
                )


        article.parentArticle?.let { checkParentPresent(it) } ?: article.valid()

    }

}


suspend fun Validator<ArticleValidationError, Article>.validate(
    id: Id<Article>?,
    title: String,
    text: String,
    rubric: Rubric,
    priority: Priority,
    targetGroup: TargetGroup,
    supportType: SupportType,
    subject: Subject,
    state: ArticleState,
    archiveDate: DateTime,
    recurrentInfo: RecurrentInfo?,
    applicationDeadline: DateTime,
    contactPartner: ContactPartner? = null,
    childArticle: Id<Article>? = null,
    parentArticle: Id<Article>? = null
) = validate(
    Article(
        id,
        title,
        text,
        rubric,
        priority,
        targetGroup,
        supportType,
        subject,
        state,
        archiveDate,
        recurrentInfo,
        applicationDeadline,
        contactPartner,
        childArticle,
        parentArticle
    )
)
