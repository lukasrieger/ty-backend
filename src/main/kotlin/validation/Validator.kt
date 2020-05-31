package validation

import arrow.Kind
import arrow.core.Nel
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.extensions.list.traverse.sequence
import arrow.core.extensions.nonemptylist.semigroup.semigroup
import arrow.core.extensions.validated.applicative.applicative
import arrow.core.fix
import arrow.fx.typeclasses.Concurrent
import arrow.fx.typeclasses.ConcurrentSyntax


typealias ValidateF<F, E, T> = ConcurrentSyntax<F>.(T) -> Kind<F, ValidatedNel<E, T>>

interface Validator<F, E, T> {

    val runtime: Concurrent<F>

    val validators: MutableList<ValidateF<F, E, T>>

    /**
     * This function performs shallow validation of some value T.
     * If the validation succeeds, this function will return the input value unchanged,
     * else the return value will be a non empty List of ValidationErrors indicating what went wrong.
     * @param value T
     * @return Validated<E,T>
     */
    fun validate(value: T): Kind<F, ValidatedNel<E, T>> = runtime.fx.concurrent {
        validators
            .map { !it(value) }
            .sequence(Validated.applicative(Nel.semigroup<E>())).fix()
            .map { value }
    }

}

/**
 * Convenience class that exposes a simple [validation] function to create new validators.
 * This validator will automatically be invoked when calling [validate].
 * @param E
 * @param T
 * @property validators MutableList<SuspendFunction1<T, Validated<NonEmptyList<E>, T>>>
 */
abstract class AbstractValidator<F, E, T> : Validator<F, E, T> {

    override val validators: MutableList<ValidateF<F, E, T>> = mutableListOf()

    protected fun validation(f: ConcurrentSyntax<F>.(T) -> Kind<F, Validated<E, T>>)
            : ConcurrentSyntax<F>.(T) -> Kind<F, ValidatedNel<E, T>> {
        val validatingFunc: ConcurrentSyntax<F>.(T) -> Kind<F, ValidatedNel<E, T>> = { value ->
            runtime.fx.concurrent { f(value).map { it.toValidatedNel() }.bind() }
        }
        validators += (validatingFunc)
        return validatingFunc
    }
}