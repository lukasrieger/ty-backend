package util

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import types.dao.ArticlesTable
import types.dao.ContactTable


object TestDbSettings {
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", "org.h2.Driver").also {
            it.useNestedTransactions = true
        }

        transaction {
            SchemaUtils.create(ArticlesTable)
            SchemaUtils.create(ContactTable)
        }

    }
}