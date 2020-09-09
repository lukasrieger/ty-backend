package model

import arrow.core.Nel
import service.Id


sealed class DatabaseError {
    data class NotFound<T>(val id: Id<T>) : DatabaseError()

    data class ValidationFailed<E>(val causes: Nel<E>): DatabaseError()

    data class UnknownError(val cause: Throwable) : DatabaseError()
}

