package types.syntax.effect

import org.jetbrains.exposed.sql.Query
import types.Id
import types.Index
import types.ReadDB
import types.WriteDB
import types.syntax.ResultErrorSyntax


fun interface QueryResultEffect<R> :
    WriteDB.ErrorSyntax<Result<*>>,
    ReadDB.ErrorSyntax<Result<*>>,
    ResultEffect<R> {

    override suspend fun <T> ReadDB<T>.trySingleById(identifier: Id<T>): Result<*> =
        with(ResultErrorSyntax) { trySingleById(identifier) }

    override suspend fun <T> ReadDB<T>.trySingleByIdOrNull(identifier: Id<T>): Result<*> =
        with(ResultErrorSyntax) { trySingleByIdOrNull(identifier) }

    override suspend fun <T> ReadDB<T>.tryGetByQuery(query: Query, limit: Int?, offset: Long?): Result<*> =
        with(ResultErrorSyntax) { tryGetByQuery(query, limit, offset) }

    override suspend fun <T : Index<T>> WriteDB<T>.tryCreate(entity: T): Result<Id<T>> =
        with(ResultErrorSyntax) { tryCreate(entity) }

    override suspend fun <T : Index<T>> WriteDB<T>.tryUpdate(entity: T): Result<Unit> =
        with(ResultErrorSyntax) { tryUpdate(entity) }

    override suspend fun <T : Index<T>> WriteDB<T>.tryDelete(entity: Id<T>): Result<Unit> =
        with(ResultErrorSyntax) { tryDelete(entity) }

}

