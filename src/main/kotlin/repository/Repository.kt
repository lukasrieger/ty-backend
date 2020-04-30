package repository

import arrow.core.Valid
import arrow.fx.IO
import org.jetbrains.exposed.sql.Query



interface Reader<T> {

    /**
     * Retrieve an entry from the database that matches the given [id].
     * The result is wrapped as an optional instance of [T],
     * indicating whether there was a match or not
     * @param id PrimaryKey<T>
     * @return Option<T>
     */
    fun byId(id: PrimaryKey<T>): IO<Throwable, T?>

    /**
     * Retrieve an arbitrary amount of entries from the database that match the given [query].
     * This function can be optionally supplied with an [offset] and [limit], which can be used for pagination.
     * The result of this function is wrapped as a [QueryResult]
     * @param limit Int?
     * @param offset Int?
     * @param query Query
     * @return QueryResult<T>
     */
    fun byQuery(query: Query, limit: Int?, offset: Long?): IO<Throwable, QueryResult<T>>

    /**
     * Returns the amount of entries in the database that match the given [query]
     * @param query Query
     * @return Int
     */
    fun countOf(query: Query): IO<Throwable, Long>

}


interface Writer<T> {

    /**
     * Modifies the given [entry] in the database.
     * Note that this function should have no effect if the entry does not yet exist in the database.
     * If there was a matching entry in the database, the entry primary key is returned.
     * @param entry T
     * @return PrimaryKey<T>
     */
    fun update(entry: Valid<T>): IO<Throwable, PrimaryKey<T>>

    /**
     * Create a new [entry] in the database.
     * This function may throw an exception, if an entry with equivalent ids already exists.
     * On success, this functions returns an updated version of [entry] with the id set to the actual key
     * @param entry T
     * @return PrimaryKey<T>
     */
    fun create(entry: Valid<T>): IO<Throwable, T>

    /**
     * Removes an entry from the database that matches the given [id].
     * This method has no effect if there is no matching entry in the database.
     * @param id PrimaryKey<T>
     * @return PrimaryKey<T>
     */
    fun delete(id: PrimaryKey<T>): IO<Throwable, PrimaryKey<T>>

}

/**
 * Generic interface for any repository that handles interaction with the database for some type [T].
 * This interface exposes writing and reading capabilities by inheriting from [Reader] and [Writer].
 * Any class that wants to implement this interface has to be a [Reader] and a [Writer]
 * @param T
 */
interface Repository<T> : Reader<T>, Writer<T>
