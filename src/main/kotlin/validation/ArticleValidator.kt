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
        if (it.applicationDeadline.isBefore(it.archiveDate)) {
            it.valid()
        } else {
            ArticleValidationError.InvalidApplicationDate(it.applicationDeadline).invalid()
        }
    }

    val validParentArticle = validation { article ->
        fun checkValidRelation(parent: Article, key: PrimaryKey<Article>) =
            if (key == article.id) {
                article.valid()
            } else {
                ArticleValidationError.InvalidRelation(parent, article).invalid()
            }

        fun checkSymmetry(parent: Article) =
            article.childArticle.fold(
                ifEmpty = { ArticleValidationError.AsymmetricRelation(parent, article).invalid() },
                ifSome = { checkValidRelation(parent, it) }
            )

        suspend fun checkParentPresent(key: PrimaryKey<Article>) =
            articleReader.byId(key).fold(
                ifEmpty = { ArticleValidationError.MissingArticle(key).invalid() },
                ifSome = ::checkSymmetry
            )

        article.parentArticle.fold(
            ifEmpty = { article.valid() },
            ifSome = { checkParentPresent(it) }
        )
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
