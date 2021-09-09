package types

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction


internal fun Query.paginate(limit: Int?, offset: Long?): Query = apply {
    if (limit != null) {
        offset?.let { limit(limit, it) } ?: limit(limit)
    }
}


/**
 * Simple helper function for running database operations in the context of some [table]
 * @param table T
 * @param f [@kotlin.ExtensionFunctionType] SuspendFunction1<T, V>
 * @return V
 */
suspend inline fun <T : Table, V> transactionEffect(table: T, crossinline f: suspend T.() -> V) =
    newSuspendedTransaction(Dispatchers.IO) {
        with(table) {
            f()
        }
    }
