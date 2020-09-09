import instances.article.defaultArticleModule
import instances.contactpartner.defaultContactPartnerModule
import org.kodein.di.DI

internal val testDiContainer = DI {
    import(defaultArticleModule)
    import(defaultContactPartnerModule)
}