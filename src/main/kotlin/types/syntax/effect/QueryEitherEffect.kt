package types.syntax.effect

import arrow.core.Either
import arrow.core.computations.EitherEffect
import org.jetbrains.exposed.sql.Query
import types.*
import types.syntax.EitherErrorSyntax

fun interface QueryEitherEffect<E, A> :
    WriteDB.ErrorSyntax<EitherWrapper>,
    ReadDB.ErrorSyntax<EitherWrapper>,
    EitherEffect<E, A> {

    override suspend fun <T> ReadDB<T>.trySingleById(identifier: Id<T>): Either<PersistenceError, T> =
        with(EitherErrorSyntax) { trySingleById(identifier) }

    override suspend fun <T> ReadDB<T>.trySingleByIdOrNull(identifier: Id<T>): Either<PersistenceError, T?> =
        with(EitherErrorSyntax) { trySingleByIdOrNull(identifier) }

    override suspend fun <T> ReadDB<T>.tryGetByQuery(
        query: Query,
        limit: Int?,
        offset: Long?
    ): Either<PersistenceError, Sequence<T>> =
        with(EitherErrorSyntax) { tryGetByQuery(query, limit, offset) }

    override suspend fun <T : Index<T>> WriteDB<T>.tryCreate(entity: T): Either<PersistenceError, Id<T>> =
        with(EitherErrorSyntax) { tryCreate(entity) }

    override suspend fun <T : Index<T>> WriteDB<T>.tryUpdate(entity: T): Either<PersistenceError, Unit> =
        with(EitherErrorSyntax) { tryUpdate(entity) }

    override suspend fun <T : Index<T>> WriteDB<T>.tryDelete(entity: Id<T>): Either<PersistenceError, Unit> =
        with(EitherErrorSyntax) { tryDelete(entity) }

}