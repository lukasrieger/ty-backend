package instances.article

import arrow.core.Either
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeTypeOf
import io.kotest.property.checkAll
import model.Article
import org.jetbrains.exposed.sql.selectAll
import types.Id
import types.PersistenceError
import types.dao.ArticlesTable
import types.syntax.EitherErrorSyntax.queryEither
import util.TestDbSettings
import util.arbitraryArticle
import kotlin.system.measureTimeMillis


class DefaultArticleReaderTest : StringSpec() {

    private val articleService: ArticleService = ArticleService

    init {

        TestDbSettings.setup()

        "byId returns NotFound when queried with invalid Id" {
            queryEither<PersistenceError, Unit> {
                val result = articleService.trySingleById(Id(0))

                // should have failed with  Either.Left<NotFound>
                result.shouldBeTypeOf<Either.Left<PersistenceError>>()

                val error = result.value

                error.shouldBeTypeOf<PersistenceError.MissingEntry<Article>>()
            }


        }

        "byIdOrNull returns null when queried with invalid id" {
            queryEither<PersistenceError, Unit> {
                val result = articleService.trySingleByIdOrNull(Id(0))

                result.shouldBeTypeOf<Either.Right<Article?>>()

                val article = result.value

                article.shouldBeNull()
            }
        }

        "byId successfully returns an article when queried with a valid Id" {
            val elapsedTime = measureTimeMillis {
                checkAll(arbitraryArticle) { article ->
                    queryEither<PersistenceError, Unit> {
                        val result = articleService.tryCreate(article)

                        result.shouldBeTypeOf<Either.Right<Id<Article>>>()

                        val (created) = result

                        val returned = articleService.trySingleById(created)

                        returned.shouldBeTypeOf<Either.Right<Article>>()
                    }
                }
            }

            println("Took $elapsedTime")

        }

        "byQuery should be fast even if query is big" {
            val elapsedTime = measureTimeMillis {
                queryEither<PersistenceError, Unit> {
                    val result = articleService.tryGetByQuery(ArticlesTable.selectAll())

                    result.shouldBeTypeOf<Either.Right<Sequence<Article>>>()

                    val articles = result.value

                    println(articles.count())
                }
            }

            println("Queried in $elapsedTime milliseconds")

        }
    }

}