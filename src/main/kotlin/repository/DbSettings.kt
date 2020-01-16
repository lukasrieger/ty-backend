package repository

import org.jetbrains.exposed.sql.Database

object DbSettings {
    fun setup() = Database.connect(
        url = "jdbc:mysql://localhost:3306/typhoon",
        driver = "com.mysql.cj.jdbc.Driver",
        user = "typhoon",
        password = "typhoon"

    ).also {
        it.useNestedTransactions = true
    }
}

