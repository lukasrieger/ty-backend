package types

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.select

@JvmInline
value class Id<T>(val identifier: Int)

data class DatabaseContext(val table: IntIdTable)

interface ReadDB<T> : FromDB<T> {
    val context: DatabaseContext

    suspend fun singleById(identifier: Id<T>): T = transactionEffect(context.table) {
        select { this@transactionEffect.id eq identifier.identifier }
            .single()
            .deserialize()
    }

    suspend fun singleByIdOrNull(identifier: Id<T>): T? = transactionEffect(context.table) {
        select { this@transactionEffect.id eq identifier.identifier }
            .singleOrNull()
            ?.deserialize()
    }

    suspend fun getByQuery(query: Query, limit: Int? = null, offset: Long? = null): Sequence<T> =
        transactionEffect(context.table) {
            query.map { it.deserialize() }
        }.asSequence()


    interface ErrorSyntax<out E> {
        suspend fun <T> ReadDB<T>.trySingleById(identifier: Id<T>): E
        suspend fun <T> ReadDB<T>.trySingleByIdOrNull(identifier: Id<T>): E
        suspend fun <T> ReadDB<T>.tryGetByQuery(query: Query, limit: Int? = null, offset: Long? = null): E
    }
}

