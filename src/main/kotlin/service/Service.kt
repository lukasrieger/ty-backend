package service

import arrow.core.Either
import arrow.core.computations.either
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import validation.Validator


interface ServiceSyntax<V, E, T> {
    val Service<V, E, T>.validator: Validator<V, T>
}

interface Service<V, E, T> : Reader<V, E, T>, ServiceSyntax<V, E, T> {

    interface Default<V, E, T> : Service<V, E, T> {

        override suspend fun update(entry: T): Either<E, Unit> = either {
            !validator.validate(entry)
                .toEither()
                .mapLeft { errorHandler.validationFailed(it) }

            !Either.catch { dataSource.update(entry) }
                .mapLeft { errorHandler.handle(it) }
        }


        override suspend fun create(entry: T): Either<E, T> = either {
            !validator.validate(entry)
                .toEither()
                .mapLeft { errorHandler.validationFailed(it) }

            !Either.catch { dataSource.create(entry) }
                .mapLeft { errorHandler.handle(it) }
        }


        override suspend fun delete(id: Id<T>): Either<E, Unit> =
            Either.catch { dataSource.delete(id) }
                .mapLeft { errorHandler.handle(it) }
    }


    /**
     * Modifies the given [entry] in the datasource.
     * Note that this function should have no effect if the entry does not yet exist in the database.
     * @param entry T
     * @return Either<E, Unit>
     */
    suspend fun update(entry: T): Either<E, Unit>

    /**
     * Create a new [entry] in the datasource.
     * This function may throw an exception, if an entry with equivalent ids already exists.
     * On success, this functions returns an updated version of [entry] with the id set to the actual key
     * @param entry T
     * @return Either<E, T>
     */
    suspend fun create(entry: T): Either<E, T>

    /**
     * Removes an entry from the datasource that matches the given [id].
     * This method has no effect if there is no matching entry in the database.
     * @param id PrimaryKey<T>
     * @return Either<E, Unit>
     */
    suspend fun delete(id: Id<T>): Either<E, Unit>

}


suspend inline fun <E : Table, T> transactionEffect(table: E, crossinline f: suspend E.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO) {
        table.run {
            f()
        }
    }


