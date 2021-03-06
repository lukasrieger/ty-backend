package validation

import arrow.core.Nel
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.extensions.list.traverse.sequence
import arrow.core.extensions.nonemptylist.semigroup.semigroup
import arrow.core.extensions.validated.applicative.applicative
import arrow.core.fix


typealias ValidateF<E, T> = suspend (T) -> ValidatedNel<E, T>

interface Validator<E, T> {

    val validators: MutableList<ValidateF<E, T>>

    /**
     * This function performs shallow validation of some value T.
     * If the validation succeeds, this function will return the input value unchanged,
     * else the return value will be a non empty List of [E] indicating what went wrong.
     * @param value T
     * @return Validated<E,T>
     */
    suspend fun validate(value: T): ValidatedNel<E, T> =
        validators
            .map { it(value) }
            .sequence(Validated.applicative(Nel.semigroup())).fix()
            .map { value }


}

/**
 * Convenience class that exposes a simple [validation] function to create new validators.
 * This validator will automatically be invoked when calling [validate].
 * @param E
 * @param T
 * @property validators MutableList<SuspendFunction1<T, Validated<NonEmptyList<E>, T>>>
 */
abstract class AbstractValidator<E, T> : Validator<E, T> {

    override val validators: MutableList<ValidateF<E, T>> = mutableListOf()

    protected fun validation(f: suspend (T) -> Validated<E, T>): suspend (T) -> ValidatedNel<E, T> {
        val validatingFunc: suspend (T) -> ValidatedNel<E, T> = { value: T -> f(value).toValidatedNel() }
        validators += (validatingFunc)
        return validatingFunc
    }
}