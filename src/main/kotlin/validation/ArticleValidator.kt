package validation

import arrow.core.None
import arrow.core.Option
import arrow.core.invalid
import arrow.core.valid
import model.*
import org.joda.time.DateTime
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.dsl.module
import repository.ArticleReader
import repository.PrimaryKey

val validationModule = module {
    single { ArticleValidator }
}


object ArticleValidator : AbstractValidator<ArticleValidationError, Article>(), KoinComponent {

    private val articleReader: ArticleReader by inject()


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


    val validParentArticle = validation { article ->
        article.parentArticle.fold(
            ifEmpty = { article.valid() },
            ifSome = { key ->
                articleReader.byId(key).fold(
                    ifEmpty = { ArticleValidationError.MissingArticle(key).invalid() },
                    ifSome = { parent ->
                        parent.childArticle.fold(
                            ifEmpty = { ArticleValidationError.AsymmetricRelation(parent, article).invalid() },
                            ifSome = {
                                if (it == article.id) {
                                    article.valid()
                                } else {
                                    ArticleValidationError.InvalidRelation(parent, article).invalid()
                                }
                            }
                        )
                    }
                )
            })
    }


}


suspend fun Validator<ArticleValidationError, Article>.validate(
    id: PrimaryKey<Article> = repository.Init,
    title: String,
    text: String,
    rubric: Rubric,
    priority: Priority,
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
