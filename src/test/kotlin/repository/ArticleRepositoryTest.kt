package repository


import arrow.core.Either.Right
import arrow.core.None
import io.kotlintest.assertions.arrow.either.shouldBeRight
import io.kotlintest.assertions.arrow.option.shouldBeNone
import io.kotlintest.assertions.arrow.option.shouldNotBeNone
import io.kotlintest.koin.KoinListener
import io.kotlintest.properties.Gen
import io.kotlintest.properties.assertAll
import io.kotlintest.properties.assertNone
import io.kotlintest.shouldThrowAny
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import model.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.joda.time.DateTime
import org.koin.test.KoinTest
import org.koin.test.inject
import repository.dao.ArticlesTable
import repository.dao.ContactTable
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis

object TestDbSettings {
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", "org.h2.Driver").also {
            it.useNestedTransactions = true
        }
        SchemaUtils.create(ArticlesTable)
        SchemaUtils.create(ContactTable)
    }
}

object ArticleGenerator : Gen<Article> {
    override fun constants(): Iterable<Article> = emptyList()

    override fun random(): Sequence<Article> = generateSequence {
        Article(
            id = keyOf(Gen.int().random().first()),
            title = Gen.string().random().first(),
            text = Gen.string().random().first(),
            rubric = Gen.enum<Rubric>().random().first(),
            priority = Gen.int().random().first(),
            targetGroup = Gen.enum<TargetGroup>().random().first(),
            supportType = Gen.enum<SupportType>().random().first(),
            subject = Gen.enum<Subject>().random().first(),
            state = Gen.enum<ArticleState>().random().first(),
            archiveDate = DateTime.now(),
            applicationDeadline = DateTime.now(),
            contactPartner = None,
            childArticle = None,
            parentArticle = None,
            recurrentInfo = None
        )
    }

}


class ArticleRepositoryTest : StringSpec(), KoinTest, CoroutineScope {

    override fun listeners() = listOf(KoinListener(listOf(articleModule, contactModule)))

    private val repo: ArticleRepository by inject()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + CoroutineName("ArticleRepositoryTest")


    init {

        TestDbSettings.setup()
        "Articles can that were created can immediately be read " {
            val time = measureTimeMillis {
                assertAll(ArticleGenerator) { article: Article ->
                    runBlocking {
                        val keyResult = repo.create(article)
                        keyResult.shouldBeRight()

                        val (createdArticle) = keyResult as Right<Article>
                        val art = repo.byId(createdArticle.id)

                        art.shouldNotBeNone()


                    }
                }
            }
            println(time)

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
                    repo.create(a)
                    repo.delete(a.id)
                    val art = repo.byId(a.id)
                    art.shouldBeNone()
                }
            }
        }
    }
}