package validation

import arrow.core.*
import arrow.core.extensions.list.traverse.traverse
import arrow.core.extensions.nonemptylist.semigroup.semigroup
import arrow.core.extensions.validated.applicative.applicative
import model.Article


object ArticleValidator : AbstractValidator<ArticleValidationError, Article>() {


    internal val validTitle = validation {
        if (it.title.isNotBlank()) {
            it.valid()
        } else {
            ArticleValidationError.BlankField(Article::title).invalid()
        }
    }

    internal val validApplicationDate = validation {
        val isBeforeArchiveDate = it.applicationDeadline.isBefore(it.archiveDate)
        if (isBeforeArchiveDate) {
            it.valid()
        } else {
            ArticleValidationError.InvalidApplicationDate.invalid()
        }
    }


    override fun validate(value: Article): ValidatedNel<ArticleValidationError, Article> = validators
        .traverse(ValidatedNel.applicative(Nel.semigroup<ArticleValidationError>())) { it(value) }.fix()
        .map { value }


}
