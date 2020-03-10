package validation

import arrow.core.*
import arrow.core.extensions.list.traverse.sequence
import arrow.core.extensions.list.traverse.traverse
import arrow.core.extensions.nonemptylist.semigroup.semigroup
import arrow.core.extensions.validated.applicative.applicative
import arrow.core.extensions.validated.applicative.map
import arrow.core.extensions.validated.applicativeError.raiseError
import arrow.core.extensions.validated.functor.map
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
            ifEmpty = { article.valid() }, // we treat the absence of a parent article as valid for now.
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




    override suspend fun validate(value: Article): ValidatedNel<ArticleValidationError, Article> =
        validators
            .map { it(value) }
            .sequence(ValidatedNel.applicative(Nel.semigroup<ArticleValidationError>()))
            .map { value }.fix()




}


suspend fun Validator<ArticleValidationError, Article>.validate(
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
