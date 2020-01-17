package model.error

sealed class QueryException {
    companion object
}

object GenericFailure : QueryException()

data class EntryExistsException(val reason: String) : QueryException()

fun QueryException.Companion.of(err: Throwable): QueryException = TODO()