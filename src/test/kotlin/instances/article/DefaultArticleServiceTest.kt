package instances.article

import arrow.core.Either
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeTypeOf
import io.kotest.property.checkAll
import model.Article
import types.Id
import types.PersistenceError
import types.ReadDBEitherSyntax.trySingleById
import types.WriteDBEitherSyntax.tryCreate
import types.WriteDBEitherSyntax.tryDelete
import types.WriteDBEitherSyntax.tryUpdate
import util.arbitraryArticle


class DefaultArticleServiceTest : StringSpec() {

    private val articleService: ArticleService = ArticleService

    init {
        "service.update should have no effect for non existing articles" {
            checkAll(arbitraryArticle) { article ->
                val negId = Id<Article>(-1)
                val withInvalidId = article.copy(id = negId)

                val result = articleService.tryUpdate(withInvalidId)

                result.shouldBeTypeOf<Either.Right<Unit>>()
                val checkNotExisting = articleService.trySingleById(negId)

                checkNotExisting.shouldBeTypeOf<Either.Left<PersistenceError.MissingEntry<Article>>>()
            }
        }

        "service.create always returns an article with an id" {
            checkAll(arbitraryArticle) { article ->
                val result = articleService.tryCreate(article)

                result.shouldBeTypeOf<Either.Right<Id<Article>>>()
            }
        }

        "service.delete should not fail for non existing ids" {
            checkAll<Int> { num ->
                val result = articleService.tryDelete(Id(num))

                result.shouldBeTypeOf<Either.Right<Unit>>()
            }
        }
    }

}