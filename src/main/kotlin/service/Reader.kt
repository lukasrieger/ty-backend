package service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import org.jetbrains.exposed.sql.Query
import service.extensions.paginate

interface ReaderSyntax<V, E, T> {
    val dataSource: DataSource<T>
    val errorHandler: ErrorHandler<V, E, T>
}

interface Reader<V, E, T> : ReaderSyntax<V, E, T> {

    interface Default<V, E, T> : Reader<V, E, T> {
        override suspend fun byId(id: Id<T>): Either<E, T> =
            byIdOrNull(id)
                .flatMap { value -> value?.right() ?: errorHandler.notFound(id).left() }


        override suspend fun byIdOrNull(id: Id<T>): Either<E, T?> =
            Either.catch { dataSource.get(id) }
                .mapLeft { errorHandler.handle(it) }


        override suspend fun byQuery(query: Query, limit: Int?, offset: Long?): Either<E, List<T>> =
            Either.catch { dataSource.get(query.paginate(limit, offset), limit, offset) }
                .mapLeft { errorHandler.handle(it) }


        override suspend fun countOf(query: Query): Either<E, Long> =
            Either.catch { dataSource.count(query) }
                .mapLeft { errorHandler.handle(it) }

    }


    /**
     * Retrieve an entry from the database that matches the given [id].
     * If the requested value does not exist, this function will return an instance of [Either.Left]
     * indicating that no value with a matching id could be found.
     * @param id Id<T>
     * @return Either<E, T>
     */
    suspend fun byId(id: Id<T>): Either<E, T>


    /**
     * Retrieve an entry from the database that matches the given [id].
     * The resulting value may be null if it does not exist.
     * @see byId
     * @param id PrimaryKey<T>
     * @return Either<E, T>
     */
    suspend fun byIdOrNull(id: Id<T>): Either<E, T?>

    /**
     * Retrieve an arbitrary amount of entries from the database that match the given [query].
     * This function can be optionally supplied with an [offset] and [limit], which can be used for pagination.
     * @param query Query
     * @param limit Int?
     * @param offset Int?
     * @return Either<E, List<T>>
     */
    suspend fun byQuery(query: Query, limit: Int? = null, offset: Long? = null): Either<E, List<T>>

    /**
     * Returns the amount of entries in the database that match the given [query]
     * @param query Query
     * @return Either<E, Long>
     */
    suspend fun countOf(query: Query): Either<E, Long>

}