package validation

import arrow.core.*
import arrow.core.extensions.list.traverse.traverse
import arrow.core.extensions.nonemptylist.semigroup.semigroup
import arrow.core.extensions.validated.applicative.applicative
import model.Article

typealias ValidatorFunction<E, T> = (T) -> ValidatedNel<E, T>

interface Validator<E, T> {
    /**
     * This function performs shallow validation of some value T.
     * @param value T
     * @return Validated<E,T>
     */
    fun validate(value: T): ValidatedNel<E, T>
}

abstract class AbstractValidator<E, T> : Validator<E, T> {

    protected val validators: MutableList<ValidatorFunction<E, T>> = mutableListOf()

    protected inline fun validation(crossinline f: (T) -> Validated<E, T>): (T) -> ValidatedNel<E, T> {
        val validatingFunc = { t: T -> f(t).toValidatedNel() }
        validators += (validatingFunc)

        return validatingFunc
    }
}

object ArticleValidator : AbstractValidator<ArticleValidationError, Article>() {


    internal val validTitle = validation {
        if (it.title.isNotBlank()) {
            it.valid()
        } else {
            ArticleValidationError.BlankField(Article::title).invalid()
        }
    }

    internal val validApplicationDate = validation {
        if (it.applicationDeadline.isAfterNow) {
            it.valid()
        } else {
            ArticleValidationError.InvalidApplicationDate.invalid()
        }
    }


    override fun validate(value: Article): ValidatedNel<ArticleValidationError, Article> = validators
        .traverse(ValidatedNel.applicative(Nel.semigroup<ArticleValidationError>())) { it(value) }.fix()
        .map { value }


}
