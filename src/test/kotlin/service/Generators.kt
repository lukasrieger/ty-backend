package service

import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import model.*
import org.joda.time.DateTime

val arbitraryArticle = arb {  rs ->
    generateSequence {

        val archiveDate = DateTime.parse(Arb.localDateTime().values(rs).first().value.toString())
        val applicationDate = archiveDate.minus(10000)
        Article(
            title = Arb.string().filter { it.isNotBlank() }.values(rs).first().value,
            text = Arb.string().values(rs).first().value,
            rubric = Arb.enum<Rubric>().values(rs).first().value,
            priority = Arb.enum<Priority>().values(rs).first().value,
            targetGroup = Arb.enum<TargetGroup>().values(rs).first().value,
            subject = Arb.enum<Subject>().values(rs).first().value,
            supportType = Arb.enum<SupportType>().values(rs).first().value,
            state = Arb.enum<ArticleState>().values(rs).first().value,
            archiveDate = archiveDate,
            recurrentInfo = null,
            applicationDeadline = applicationDate,
            contactPartner = null,
            childArticle = null,
            parentArticle = null,
            attachedSource = null,
        )
    }


}