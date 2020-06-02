package validation

import arrow.core.invalid
import arrow.core.valid
import arrow.fx.IO
import arrow.fx.extensions.io.concurrent.concurrent
import arrow.fx.typeclasses.Concurrent
import model.*
import org.joda.time.DateTime
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.dsl.module
import repository.ArticleReader
import repository.PrimaryKey

val validationModule = module {
    single { ArticleValidator(IO.concurrent()) }
}


class ArticleValidator<F>(override val runtime: Concurrent<F>) :
    AbstractValidator<F, ArticleValidationError, Article>(), KoinComponent {

    private val articleReader: ArticleReader<F> by inject()

    val validTitle = validation {
        if (it.title.isNotBlank()) {
            just(it.valid())
        } else {
            just(ArticleValidationError.BlankField(Article::title).invalid())
        }
    }

    val validApplicationDate = validation {
        if (it.applicationDeadline.isBefore(it.archiveDate)) {
            just(it.valid())
        } else {
            just(ArticleValidationError.InvalidApplicationDate(it.applicationDeadline).invalid())
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
            article.childArticle?.let { checkValidRelation(parent, it) } ?: ArticleValidationError.AsymmetricRelation(
                parent,
                article
            ).invalid()


        fun Concurrent<F>.checkParentPresent(key: PrimaryKey<Article>) =
            fx.concurrent {
                (!articleReader.byId(key))?.let(::checkSymmetry)
                    ?: ArticleValidationError.MissingArticle(key).invalid()
            }

        article.parentArticle?.let { checkParentPresent(it) } ?: just(article.valid())

    }

}


fun <F> Validator<F, ArticleValidationError, Article>.validate(
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
    recurrentInfo: RecurrentInfo?,
    applicationDeadline: DateTime,
    contactPartner: ContactPartner? = null,
    childArticle: PrimaryKey<Article>? = null,
    parentArticle: PrimaryKey<Article>? = null
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
