package instances.article

import arrow.core.Either
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.kotest.property.checkAll
import model.Article
import model.DatabaseError
import org.jetbrains.exposed.sql.selectAll
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import service.Service
import service.TestDbSettings
import service.arbitraryArticle
import service.dao.ArticlesTable
import service.id
import testDiContainer
import kotlin.system.measureTimeMillis


typealias ArticleService = Service<ArticleValidationError, DatabaseError, Article>

class DefaultArticleReaderTest : StringSpec(), DIAware {

    override val di: DI = testDiContainer
    private val articleService: ArticleService by instance()


    init {

        TestDbSettings.setup()

        "byId returns NotFound when queried with invalid Id" {
            val result = articleService.byId(0.id())

            // should have failed with  Either.Left<NotFound>
            result.shouldBeTypeOf<Either.Left<DatabaseError>>()

            val error = result.a

            error.shouldBeTypeOf<DatabaseError.NotFound<Article>>()

            error.id shouldBe 0.id()

        }

        "byIdOrNull returns null when queried with invalid id" {
            val result = articleService.byIdOrNull(0.id())

            result.shouldBeTypeOf<Either.Right<Article?>>()

            val article = result.b

            article.shouldBeNull()
        }

        "byId successfully returns an article when queried with a valid Id" {
            val elapsedTime = measureTimeMillis {
                checkAll(arbitraryArticle) { article ->
                    val result = articleService.create(article)

                    result.shouldBeTypeOf<Either.Right<Article>>()

                    val created = result.b

                    val returned = articleService.byId(created.id)

                    returned.shouldBeTypeOf<Either.Right<Article>>()
                }
            }

            println("Took $elapsedTime")

        }

        "byQuery should be fast even if query is big" {
            val elapsedTime = measureTimeMillis {
                val result = articleService.byQuery(ArticlesTable.selectAll())

                result.shouldBeTypeOf<Either.Right<List<Article>>>()

                val articles = result.b

                println(articles.size)
            }

            println("Queried in $elapsedTime milliseconds")

        }

        "countOf should never fail for any valid query" {
            val result = articleService.countOf(ArticlesTable.selectAll())

            result.shouldBeTypeOf<Either.Right<Long>>()

            println(result.b)
        }
    }

}