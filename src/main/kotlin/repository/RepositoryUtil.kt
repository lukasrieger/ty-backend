package repository

import arrow.core.*
import arrow.core.None
import arrow.syntax.function.pipe
import model.error.QueryException
import model.error.leftOf
import org.jetbrains.exposed.dao.EntityID
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


/**
 * Convenient alias for the Either type. Heavily used by the repository layer.
 */
typealias Result<T> = Either<QueryException, T>

/**
 * This class describes an arbitrary ordering over some column of a table.
 * Can be either ascending or descending.
 * @param T
 * @param S : SortOrder
 * @property ord Pair<Column<T>, S>
 * @constructor
 */
class Ordering<T, S : SortOrder>(val ord: Pair<Column<T>, S>) {
    operator fun component1() = ord
}

/**
 * Convenience function to construct a new ordering.
 * Should be preferred over directly instantiating objects of Ordering.
 * @param ord Function0<Pair<Column<T>, S>>
 * @return Ordering<T, S>
 */
fun <T, S : SortOrder> orderOf(ord: () -> Pair<Column<T>, S>) = Ordering(ord())

/**
 * Lifts the receiver from the general kotlin.Result type into our own Result type
 * (which is currently just a typealias around arrows Either).
 * @receiver kotlin.Result<T>
 * @return Result<T>
 */
internal fun <T> kotlin.Result<T>.foldEither(): Result<T> = fold(
    onSuccess = ::Right,
    onFailure = { leftOf(it) }
)

/**
 * Attempts to construct some type [T] from the receiver ResultRow.
 * If the receiver is null, this function returns None (else Some<[T]>).
 * @receiver ResultRow?
 * @param builder [@kotlin.ExtensionFunctionType] SuspendFunction1<ResultRow, T>
 * @return Option<T>
 */
internal suspend inline fun <T> ResultRow?.asOption(crossinline builder: suspend ResultRow.() -> T): Option<T> =
    when (this) {
        null -> None
        else -> this.builder() pipe ::Some
    }

internal inline fun <T> ResultRow?.asOption(builder: ResultRow.() -> T): Option<T> = when (this) {
    null -> None
    else -> this.builder() pipe ::Some
}


internal operator fun EntityID<Int>.component1() = value