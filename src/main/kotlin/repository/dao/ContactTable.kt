package repository.dao

import org.jetbrains.exposed.dao.id.IntIdTable

/**
 * Table definition with rows that correspond to the properties of the [model.ContactPartner] type.
 */
object ContactTable : IntIdTable() {

    val firstName = varchar("firstName", 50)
    val lastName = varchar("lastname", 50)
    val phoneNumber = varchar("phoneNumber", 50)
    val url = text("url")

}