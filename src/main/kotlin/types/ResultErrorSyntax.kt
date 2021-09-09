package types

import org.jetbrains.exposed.sql.Query

object ReadDBResultSyntax : ReadDB.ErrorSyntax<Result<*>> {
    override suspend fun <T> ReadDB<T>.trySingleById(identifier: Id<T>): Result<T> =
        kotlin.runCatching { singleById(identifier) }

    override suspend fun <T> ReadDB<T>.trySingleByIdOrNull(identifier: Id<T>): Result<T?> =
        kotlin.runCatching { singleByIdOrNull(identifier) }

    override suspend fun <T> ReadDB<T>.tryGetByQuery(query: Query, limit: Int?, offset: Long?): Result<Sequence<T>> =
        kotlin.runCatching { getByQuery(query, limit, offset) }
}


object WriteDBResultSyntax : WriteDB.ErrorSyntax<Result<*>> {
    override suspend fun <T : Index<T>> WriteDB<T>.tryCreate(entity: T): Result<Id<T>> =
        kotlin.runCatching { create(entity) }

    override suspend fun <T : Index<T>> WriteDB<T>.tryUpdate(entity: T): Result<Unit> =
        kotlin.runCatching { update(entity) }

    override suspend fun <T : Index<T>> WriteDB<T>.tryDelete(entity: Id<T>): Result<Unit> =
        kotlin.runCatching { delete(entity) }
}


object ResultErrorSyntax :
    ReadDB.ErrorSyntax<Result<*>> by ReadDBResultSyntax,
    WriteDB.ErrorSyntax<Result<*>> by WriteDBResultSyntax

