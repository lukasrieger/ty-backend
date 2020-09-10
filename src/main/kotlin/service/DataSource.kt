package service

import org.jetbrains.exposed.sql.Query

interface DataSource<T> {

    suspend fun get(id: Id<T>): T?

    suspend fun get(query: Query, limit: Int? = null, offset: Long? = null): List<T>

    suspend fun count(query: Query): Long

    suspend fun update(value: T)

    suspend fun create(value: T): T

    suspend fun delete(id: Id<T>)
}