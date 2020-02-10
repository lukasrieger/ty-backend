package repository

import arrow.core.*
import arrow.core.None
import arrow.syntax.function.pipe
import model.error.QueryException
import model.error.leftOf
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder


/**
 * This class encapsulates a result that was returned by some database query.
 * Note that count DOES NOT reflect, whether the original query was limited
 * or had an offset. Count always represents the real amount of matches, that were found for
 * the given query.
 * @param T
 * @property count Int
 * @property result Collection<T>
 * @constructor
 */
data class QueryResult<T>(val count: Int, val result: Collection<T>)

typealias Result<T> = Either<QueryException, T>

class Ordering<T, S : SortOrder>(val ord: Pair<Column<T>, S>) {
    operator fun component1() = ord
}

fun <T, S : SortOrder> orderOf(ord: () -> Pair<Column<T>, S>) = Ordering(ord())


internal fun <T> kotlin.Result<T>.foldEither(): Result<T> = fold(
    onSuccess = ::Right,
    onFailure = { leftOf(it) }
)

internal suspend inline fun <T> ResultRow?.asOption(crossinline builder: suspend ResultRow.() -> T): Option<T> =
    when (this) {
        null -> None
        else -> this.builder() pipe ::Some
    }

internal inline fun <T> ResultRow?.asOption(builder: ResultRow.() -> T): Option<T> = when (this) {
    null -> None
    else -> this.builder() pipe ::Some
}
