package util

import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.localDateTime
import io.kotest.property.arbitrary.string
import model.*
import org.joda.time.DateTime


/**
 * If [DateTime.parse] throws an exception, its probably because the given LocalDateTime
 * can't be converted to a [DateTime] -> In that case, recurse and try again with a new random value.
 */
fun genArchiveDate(): DateTime = try {
    DateTime.parse(
        Arb.localDateTime()
            .samples()
            .first()
            .value
            .toString()
    )
} catch (err: IllegalArgumentException) {
    genArchiveDate()
} catch (err: UnsupportedOperationException) {
    genArchiveDate()
}

val arbitraryArticle = Arb.bind(
    Arb.string(),
    Arb.enum<Rubric>(),
    Arb.enum<Priority>(),
    Arb.enum<TargetGroup>(),
    Arb.enum<Subject>(),
    Arb.enum<SupportType>(),
    Arb.enum<ArticleState>()
) { arbText, rubric, priority, target, subject, support, state ->

    val archiveDate = genArchiveDate()
    val applicationDate = archiveDate.minus(10000)


    Article(
        title = arbText,
        text = arbText,
        rubric = rubric,
        priority = priority,
        targetGroup = target,
        subject = subject,
        supportType = support,
        state = state,
        archiveDate = archiveDate,
        recurrentInfo = null,
        applicationDeadline = applicationDate,
        contactPartner = null,
        childArticle = null,
        parentArticle = null,
        attachedSource = null,
    )
}
