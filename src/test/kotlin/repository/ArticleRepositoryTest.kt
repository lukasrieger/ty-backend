package repository

import arrow.core.None
import arrow.core.ValidatedNel
import arrow.fx.ForIO
import arrow.fx.fix
import io.kotlintest.koin.KoinListener
import io.kotlintest.properties.assertAll
import io.kotlintest.properties.assertNone
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrowAny
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import model.Article
import org.koin.test.KoinTest
import org.koin.test.inject
import validation.ArticleValidationError
import validation.ArticleValidator
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis


class ArticleRepositoryTest : StringSpec(), KoinTest, CoroutineScope {

    override fun listeners() = listOf<KoinListener>()

    // listOf(KoinListener(listOf(articleModule, contactModule, validationModule)))
    private val repo: ArticleRepository<ForIO> by inject()
    private val validator: ArticleValidator<ForIO> by inject()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + CoroutineName("repository.ArticleRepositoryTest")

    init {
        TestDbSettings.setup()
        "Articles that were created can immediately be read " {
            val time = measureTimeMillis {
                assertAll(ArticleGenerator) { article: Article ->
                    runBlocking {
                        val articleOk: ValidatedNel<ArticleValidationError, Article> =
                            validator.validate(article).fix().suspended()

                        articleOk
                            .foldV(
                                fe = {},
                                fa = {
                                    val keyResult = repo.create(it)

                                    println(keyResult)

                                    // val (createdArticle) = keyResult as Right<Article>
                                    // val art = repo.byId(createdArticle.id)
                                }
                            )
                    }
                }
            }
            println(time)
        }

        "Article validation returns usable information" {
            assertAll(ArticleGenerator) { article: Article ->
                runBlocking {
                    val articleOk = validator.validate(article).fix().suspended()
                    articleOk.leftMap {
                        it.all.forEach(::println)
                        it
                    }
                }

            }
        }

        "Querying a non existent article never throws an exception" {
            assertNone(ArticleGenerator) { article: Article ->
                runBlocking {
                    shouldThrowAny {
                        repo.byId(article.id)
                    }
                }
            }
        }

        "update" { }

        "create" { }

        "countOf" { }

        "A deleted article should not by queryable anymore" {
            assertAll(ArticleGenerator) { a: Article ->
                runBlocking {

                    val articleOk = validator.validate(a).fix().suspended()
                    articleOk.foldV(
                        fe = {},
                        fa = {
                            repo.create(it)
                            repo.delete(a.id)
                            val art = repo.byId(a.id).fix().suspended()
                            art shouldBe None
                        }
                    )
                }
            }
        }
    }
}
