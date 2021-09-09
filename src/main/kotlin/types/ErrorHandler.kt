package types

import arrow.core.Nel
import validation.Validate


fun interface ErrorHandler<E> {
    fun Throwable.toDomainError(): E
}

interface ValidateHandler<T, V, E> : ErrorHandler<E>, Validate<V, T> {

    fun Nel<V>.toDomainError(): E

    fun T.validateInDomain() = this.validate().toEither().mapLeft { it.toDomainError() }

    companion object {
        operator fun <T, V, E> invoke(
            validate: Validate<V, T>,
            errorHandler: ErrorHandler<E>,
            f: Nel<V>.() -> E
        ): ValidateHandler<T, V, E> =
            object : ValidateHandler<T, V, E>, Validate<V, T> by validate, ErrorHandler<E> by errorHandler {
                override fun Nel<V>.toDomainError() = f()
            }
    }
}

