package repository

import arrow.core.None
import io.kotlintest.properties.Gen
import model.*
import org.joda.time.DateTime


object ArticleGenerator : Gen<Article> {
    override fun constants(): Iterable<Article> = emptyList()

    override fun random(): Sequence<Article> = generateSequence {
        Article(
            id = keyOf(Gen.int().random().first()),
            title = "", //Gen.string().random().first()
            text = Gen.string().random().first(),
            rubric = Gen.enum<Rubric>().random().first(),
            priority = Gen.enum<Priority>().random().first(),
            targetGroup = Gen.enum<TargetGroup>().random().first(),
            supportType = Gen.enum<SupportType>().random().first(),
            subject = Gen.enum<Subject>().random().first(),
            state = Gen.enum<ArticleState>().random().first(),
            archiveDate = DateTime.now(),
            applicationDeadline = DateTime.now().plusDays(4),
            contactPartner = None,
            childArticle = None,
            parentArticle = None,
            recurrentInfo = None
        )
    }
}