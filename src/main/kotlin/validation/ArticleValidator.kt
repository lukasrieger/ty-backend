package validation

import arrow.core.*
import arrow.core.extensions.list.traverse.traverse
import arrow.core.extensions.nonemptylist.semigroup.semigroup
import arrow.core.extensions.validated.applicative.applicative
import arrow.syntax.function.pipe
import model.*
import org.joda.time.DateTime
import repository.PrimaryKey


object ArticleValidator : AbstractValidator<ArticleValidationError, Article>() {


    val validTitle = validation {
        if (it.title.isNotBlank()) {
            it.valid()
        } else {
            ArticleValidationError.BlankField(Article::title).invalid()
        }
    }

    val validApplicationDate = validation {
        val isBeforeArchiveDate = it.applicationDeadline.isBefore(it.archiveDate)
        if (isBeforeArchiveDate) {
            it.valid()
        } else {
            ArticleValidationError.InvalidApplicationDate.invalid()
        }
    }


    override fun validate(value: Article): ValidatedNel<ArticleValidationError, Article> = validators
        .traverse(ValidatedNel.applicative(Nel.semigroup<ArticleValidationError>())) { it(value) }.fix()
        .map { value }


}


fun Validator<ArticleValidationError, Article>.validate(
    id: PrimaryKey<Article> = repository.None,
    title: String,
    text: String,
    rubric: Rubric,
    priority: Int,
    targetGroup: TargetGroup,
    supportType: SupportType,
    subject: Subject,
    state: ArticleState,
    archiveDate: DateTime,
    recurrentInfo: Option<RecurrentInfo>,
    applicationDeadline: DateTime,
    contactPartner: Option<ContactPartner> = None,
    childArticle: Option<PrimaryKey<Article>> = None,
    parentArticle: Option<PrimaryKey<Article>> = None
) = Article(
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
) pipe ::validate
