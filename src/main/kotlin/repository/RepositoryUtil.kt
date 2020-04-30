package repository

import arrow.core.*
import arrow.syntax.function.pipe
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction


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
data class QueryResult<T>(val count: Long, val result: Collection<T>)


/**
 * This class describes an arbitrary ordering over some column of a table.
 * Can be either ascending or descending.
 * @param T
 * @param S : SortOrder
 * @property ord Pair<Column<T>, S>
 * @constructor
 */
data class Ordering<T, S : SortOrder>(val ord: Pair<Column<T>, S>)

/**
 * Convenience function to construct a new ordering.
 * Should be preferred over directly instantiating objects of Ordering.
 * @param ord Function0<Pair<Column<T>, S>>
 * @return Ordering<T, S>
 */
fun <T, S : SortOrder> orderOf(ord: () -> Pair<Column<T>, S>) = Ordering(ord())


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

/**
 * This function behaves exactly like a normal map, except that [f] receives the Valid wrapper instead of the
 * value contained within.
 * @receiver Validated<E, A>
 * @param f Function1<Valid<A>, B>
 * @return Validated<E, B>
 */
inline fun <E, A, B> Validated<E, A>.mapV(crossinline f: (Valid<A>) -> B): Validated<E, B> =
    bimap(::identity) { f(Valid(it)) }

suspend inline fun <E, A, B> Validated<E, A>.mapAsyncV(crossinline f: suspend (Valid<A>) -> B): Validated<E, B> {
    val value = (this as Valid<A>).a
    val mapped = f(Valid(value))

    return bimap({ left: E -> left }) { mapped }
}


/**
 * This function behaves exactly like a normal fold, except that [fa] receives the Valid wrapper instead of the
 * value contained within.
 * @receiver Validated<E,A>
 * @param fe Function1<E, B>
 * @param fa Function1<Valid<A>, B>
 * @return B
 */
inline fun <E, A, B> Validated<E, A>.foldV(fe: (E) -> B, fa: (Valid<A>) -> B) = when (this) {
    is Validated.Valid -> fa(this)
    is Validated.Invalid -> (fe(e))
}

suspend fun <E : Table, T> safeTransactionIO(table: E, f: E.() -> T): Either<Throwable, T> = Either.catch {
    newSuspendedTransaction(Dispatchers.IO) { table.run(f) }
}
