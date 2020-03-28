package repository.dao

import org.jetbrains.exposed.dao.id.IntIdTable


object ContactTable : IntIdTable() {

    val firstName = varchar("firstName", 50)
    val lastName = varchar("lastName", 50)
    val phoneNumber = varchar("phoneNumber", 50)
    val url = text("url")

}