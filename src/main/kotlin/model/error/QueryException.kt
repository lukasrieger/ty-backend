package model.error

import arrow.core.Either
import arrow.core.Either.Companion.left
import arrow.syntax.function.pipe


data class QueryException(val reason: Throwable)

internal fun errOf(err: Throwable): QueryException = QueryException(err)

internal fun <T> leftOf(err: Throwable): Either<QueryException, T> = errOf(err) pipe ::left
