package model.error

import arrow.core.Either


data class QueryException(val reason: Throwable)

internal fun errOf(err: Throwable): QueryException = QueryException(err)

internal fun <T> leftOf(err: Throwable): Either<QueryException, T> = Either.left(errOf(err))