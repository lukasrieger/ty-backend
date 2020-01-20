package repository

import arrow.core.Either
import arrow.core.Option
import model.error.QueryException
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Query
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


/**
 * Generic interface for any repository that handles interaction with the database for some type [T]
 * @param T
 */
interface Repository<T> {

    /**
     * Retrieve an entry from the database that matches the given [id].
     * The result is wrapped as an optional instance of [T],
     * indicating whether there was a match or not
     * @param id PrimaryKey<T>
     * @return Option<T>
     */
    suspend fun byId(id: PrimaryKey<T>): Option<T>

    /**
     * Retrieve an arbitrary amount of entries from the database that match the given [query].
     * This function can be optionally supplied with an [offset] and [limit], which can be used for pagination.
     * The result of this function is wrapped as a [QueryResult]
     * @param limit Int?
     * @param offset Int?
     * @param query Query
     * @return QueryResult<T>
     */
    suspend fun byQuery(query: Query, limit: Int?, offset: Int?): QueryResult<T>

    /**
     * Modifies the given [entry] in the database.
     * Note that this function should have no effect if the entry does not yet exist in the database.
     * If there was a matching entry in the database, the entry primary key is returned.
     * @param entry T
     * @return PrimaryKey<T>
     */
    suspend fun update(entry: T): Result<PrimaryKey<T>>

    /**
     * Create a new [entry] in the database.
     * This function may throw an exception, if an entry with equivalent ids already exists.
     * @param entry T
     * @return PrimaryKey<T>
     */
    suspend fun create(entry: T): Result<PrimaryKey<T>>

    /**
     * Removes an entry from the database that matches the given [id].
     * This method has no effect if there is no matching entry in the database.
     * @param id PrimaryKey<T>
     * @return PrimaryKey<T>
     */
    suspend fun delete(id: PrimaryKey<T>): Result<PrimaryKey<T>>

    /**
     * Returns the amount of entries in the database that match the given [query]
     * @param query Query
     * @return Int
     */
    suspend fun countOf(query: Query): Int


}