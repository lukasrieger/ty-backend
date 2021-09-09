package types

import arrow.core.Either
import org.jetbrains.exposed.sql.Query


object ReadDBEitherSyntax : ReadDB.ErrorSyntax<EitherWrapper> {
    override suspend fun <T> ReadDB<T>.trySingleById(identifier: Id<T>): Either<PersistenceError, T> =
        Either.catch { singleById(identifier) }.mapLeft { err ->
            when (err) {
                is NoSuchElementException -> PersistenceError.MissingEntry(identifier)
                else -> PersistenceError.QueryFailed(err)
            }
        }

    override suspend fun <T> ReadDB<T>.trySingleByIdOrNull(identifier: Id<T>): Either<PersistenceError, T?> =
        Either.catch { singleByIdOrNull(identifier) }.mapLeft { PersistenceError.QueryFailed(it) }

    override suspend fun <T> ReadDB<T>.tryGetByQuery(
        query: Query,
        limit: Int?,
        offset: Long?
    ): Either<PersistenceError, Sequence<T>> =
        Either.catch { getByQuery(query, limit, offset) }.mapLeft { PersistenceError.QueryFailed(it) }

}

object WriteDBEitherSyntax : WriteDB.ErrorSyntax<EitherWrapper> {
    override suspend fun <T : Index<T>> WriteDB<T>.tryCreate(entity: T): Either<PersistenceError, Id<T>> =
        Either.catch { create(entity) }.mapLeft { PersistenceError.QueryFailed(it) }

    override suspend fun <T : Index<T>> WriteDB<T>.tryUpdate(entity: T): Either<PersistenceError, Unit> =
        Either.catch { update(entity) }.mapLeft { PersistenceError.QueryFailed(it) }

    override suspend fun <T : Index<T>> WriteDB<T>.tryDelete(entity: Id<T>): Either<PersistenceError, Unit> =
        Either.catch { delete(entity) }.mapLeft { PersistenceError.QueryFailed(it) }
}

object EitherErrorSyntax :
    ReadDB.ErrorSyntax<EitherWrapper> by ReadDBEitherSyntax,
    WriteDB.ErrorSyntax<EitherWrapper> by WriteDBEitherSyntax

