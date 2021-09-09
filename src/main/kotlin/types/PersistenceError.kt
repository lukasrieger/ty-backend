package types

import arrow.core.Either
import arrow.core.Nel

sealed interface PersistenceError {
    data class MissingEntry<T>(val id: Id<T>) : PersistenceError
    data class QueryFailed(val error: Throwable) : PersistenceError
    data class ValidationFailed<E>(val errors: Nel<E>) : PersistenceError
}

typealias EitherWrapper = Either<PersistenceError, *>