package repository

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import repository.dao.ArticlesTable
import repository.dao.ContactTable


object TestDbSettings {
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", "org.h2.Driver").also {
            it.useNestedTransactions = true
        }
        SchemaUtils.create(ArticlesTable)
        SchemaUtils.create(ContactTable)
    }
}