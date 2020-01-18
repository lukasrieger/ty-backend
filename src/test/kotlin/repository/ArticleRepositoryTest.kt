package repository

import arrow.core.None
import io.kotlintest.properties.assertAll
import io.kotlintest.properties.assertNone
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import model.Article
import org.koin.core.KoinComponent
import org.koin.core.inject
import kotlin.coroutines.CoroutineContext

class ArticleRepositoryTest() : StringSpec(), KoinComponent, CoroutineScope {

    private val repo: Repository<Article> by inject()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + CoroutineName("ArticleRepositoryTest")


    init {
        "Any kind of article should be queryable by its id" {
            assertNone { a: Article ->
                launch {
                    shouldThrow<Exception> {
                        repo.byId(a.id)
                    }
                }
            }
        }

        "byQuery" { }

        "update" { }

        "create" { }

        "countOf" { }

        "A deleted article should not by queryable anymore" {
            assertAll { a: Article ->
                launch {
                    repo.create(a)
                    repo.delete(a.id)
                    val art = repo.byId(a.id)
                    art shouldBe None
                }
            }
        }
    }
}