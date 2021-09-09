package instances.contactpartner

import model.ContactPartner
import types.*
import types.dao.ContactTable


val ContactPartnerSerializer = IntoDB<ContactPartner> { statement ->
    statement.also { stm ->
        stm[ContactTable.firstName] = surname
        stm[ContactTable.lastName] = lastName
        stm[ContactTable.phoneNumber] = phoneNumber
        stm[ContactTable.url] = url
    }
}

val ContactPartnerDeserializer = FromDB {
    ContactPartner(
        id = this[ContactTable.id].value.let(::Id),
        surname = this[ContactTable.firstName],
        lastName = this[ContactTable.lastName],
        phoneNumber = this[ContactTable.phoneNumber],
        url = this[ContactTable.url]
    )
}


object ContactPartnerReader : ReadDB<ContactPartner>, FromDB<ContactPartner> by ContactPartnerDeserializer {
    override val context: DatabaseContext = DatabaseContext(ContactTable)
}

object ContactPartnerWriter : WriteDB<ContactPartner>, IntoDB<ContactPartner> by ContactPartnerSerializer {
    override val context: DatabaseContext = DatabaseContext(ContactTable)
}