package repository

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import repository.dao.ArticlesTable
import repository.dao.ContactTable

object DbSettings {
    fun setup() =
//        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", "org.h2.Driver").also {
//            it.useNestedTransactions = true
//        }
        Database.connect(
            url = "jdbc:mysql://localhost:3306/typhoon?serverTimezone=UTC",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "typhoon",
            password = "typhoon"

        ).also {

        it.useNestedTransactions = true
            transaction {
                //
                SchemaUtils.create(ContactTable)
                SchemaUtils.create(ArticlesTable)

            }
        }
}

