package model.extensions

import model.Coerce
import model.ContactPartner
import org.jetbrains.exposed.sql.ResultRow
import repository.dao.ContactTable
import repository.keyOf

private object ContactPartnerFromResultRow : Coerce<ResultRow, ContactPartner> {
    override suspend fun ResultRow.coerce(): ContactPartner = ContactPartner(
        id = keyOf(this[ContactTable.id].value),
        surname = this[ContactTable.firstName],
        lastName = this[ContactTable.lastName],
        phoneNumber = this[ContactTable.phoneNumber],
        url = this[ContactTable.url]
    )
}

val ContactPartner.Companion.fromResultRow: Coerce<ResultRow, ContactPartner>
    get() = ContactPartnerFromResultRow


