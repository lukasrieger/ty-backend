package types.syntax

import arrow.continuations.Effect
import org.jetbrains.exposed.sql.Query
import types.Id
import types.Index
import types.ReadDB
import types.WriteDB
import types.syntax.effect.QueryResultEffect


object ResultErrorSyntax :
    ReadDB.ErrorSyntax<Result<*>>,
    WriteDB.ErrorSyntax<Result<*>> {

    suspend inline fun <R> queryResult(noinline c: suspend QueryResultEffect<*>.() -> R): Result<R> =
        Effect.suspended(eff = { QueryResultEffect { it } }, f = c, just = { Result.success(it) })

    override suspend fun <T> ReadDB<T>.trySingleById(identifier: Id<T>): Result<T> =
        kotlin.runCatching { singleById(identifier) }

    override suspend fun <T> ReadDB<T>.trySingleByIdOrNull(identifier: Id<T>): Result<T?> =
        kotlin.runCatching { singleByIdOrNull(identifier) }

    override suspend fun <T> ReadDB<T>.tryGetByQuery(query: Query, limit: Int?, offset: Long?): Result<Sequence<T>> =
        kotlin.runCatching { getByQuery(query, limit, offset) }


    override suspend fun <T : Index<T>> WriteDB<T>.tryCreate(entity: T): Result<Id<T>> =
        kotlin.runCatching { create(entity) }

    override suspend fun <T : Index<T>> WriteDB<T>.tryUpdate(entity: T): Result<Unit> =
        kotlin.runCatching { update(entity) }

    override suspend fun <T : Index<T>> WriteDB<T>.tryDelete(entity: Id<T>): Result<Unit> =
        kotlin.runCatching { delete(entity) }
}
