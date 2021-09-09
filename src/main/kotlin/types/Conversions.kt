package types

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder

/**
 * IntoDB
 *
 * @param T
 */
fun interface IntoDB<T> {
    fun T.serialize(statement: UpdateBuilder<Int>): UpdateBuilder<Int>
}

/**
 * FromDB
 *
 * @param T
 */
fun interface FromDB<T> {
    fun ResultRow.deserialize(): T
}