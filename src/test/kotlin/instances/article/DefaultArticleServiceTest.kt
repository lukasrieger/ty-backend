package instances.article

import arrow.core.Either
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeTypeOf
import io.kotest.property.checkAll
import model.Article
import model.DatabaseError
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import service.arbitraryArticle
import service.id
import testDiContainer


class DefaultArticleServiceTest : StringSpec(), DIAware {

    override val di: DI = testDiContainer

    private val articleService: ArticleService by instance()

    init {
        "update should have no effect for non existing articles" {
            checkAll(arbitraryArticle) { article ->
                val withInvalidId = article.copy(id = (-1).id())

                val result = articleService.update(withInvalidId)

                result.shouldBeTypeOf<Either.Right<Unit>>()

                val checkNotExisting = articleService.byId((-1).id())

                checkNotExisting.shouldBeTypeOf<Either.Left<DatabaseError.NotFound<Article>>>()
            }
        }

        "create always returns an article with an id" {
            checkAll(arbitraryArticle) { article ->
                val result = articleService.create(article)


                result.shouldBeTypeOf<Either.Right<Article>>()

                val created = result.b


                created.id.shouldNotBeNull()
            }
        }

        "delete should have no effect for non existing ids" {
            checkAll<Int> { num ->
                val result = articleService.delete(num.id())

                result.shouldBeTypeOf<Either.Right<Unit>>()
            }
        }
    }

}