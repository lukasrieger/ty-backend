package instances.article

import arrow.core.Either
import model.Article
import validation.Validate


fun Article.validateTitle() = Either.conditionally(
    title.isNotBlank(),
    ifFalse = { ArticleValidationError.BlankField(Article::title) },
    ifTrue = { this }
).toValidated()


fun Article.validateApplicationDate() = Either.conditionally(
    applicationDeadline.isBefore(archiveDate),
    ifFalse = { ArticleValidationError.InvalidApplicationDate(applicationDeadline) },
    ifTrue = { this }
).toValidated()

val ArticleValidate = Validate.combine(Article::validateTitle, Article::validateApplicationDate)
