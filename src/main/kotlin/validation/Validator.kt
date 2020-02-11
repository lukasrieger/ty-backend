package validation

import arrow.core.Validated
import arrow.core.ValidatedNel


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