package validation

import arrow.core.Nel
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.extensions.list.traverse.sequence
import arrow.core.extensions.nonemptylist.semigroup.semigroup
import arrow.core.extensions.validated.applicative.applicative
import arrow.core.extensions.validated.functor.map


typealias ValidatorFunction<E, T> = suspend (T) -> ValidatedNel<E, T>

interface Validator<E, T> {

    val validators: MutableList<ValidatorFunction<E, T>>

    /**
     * This function performs shallow validation of some value T.
     * @param value T
     * @return Validated<E,T>
     */
    suspend fun validate(value: T): ValidatedNel<E, T> =
        validators
            .map { it(value) }
            .sequence(ValidatedNel.applicative(Nel.semigroup<E>()))
            .map { value }


}

abstract class AbstractValidator<E, T> : Validator<E, T> {

    override val validators: MutableList<ValidatorFunction<E, T>> = mutableListOf()

    protected fun validation(f: suspend (T) -> Validated<E, T>): suspend (T) -> ValidatedNel<E, T> {
        val validatingFunc: suspend (T) -> ValidatedNel<E, T> = { t: T -> f(t).toValidatedNel() }

        validators += (validatingFunc)

        return validatingFunc
    }
}