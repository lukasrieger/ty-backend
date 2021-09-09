package types

import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update


interface Index<T> {
    val id: Id<T>?
}


interface WriteDB<T : Index<T>> : IntoDB<T> {
    val context: DatabaseContext

    suspend fun create(entity: T): Id<T> = transactionEffect(context.table) {
        Id(insert { entity.serialize(it) }[id].value)
    }

    suspend fun update(entity: T): Unit = transactionEffect(context.table) {
        update({ id eq entity.id?.identifier }) { entity.serialize(it) }
    }

    suspend fun delete(entity: Id<T>): Unit = transactionEffect(context.table) {
        deleteWhere { id eq entity.identifier }
    }

    interface ErrorSyntax<out E> {
        suspend fun <T : Index<T>> WriteDB<T>.tryCreate(entity: T): E
        suspend fun <T : Index<T>> WriteDB<T>.tryUpdate(entity: T): E
        suspend fun <T : Index<T>> WriteDB<T>.tryDelete(entity: Id<T>): E
    }
}


