package instances.contactpartner


import model.ContactPartner
import org.jetbrains.exposed.sql.selectAll
import types.ReadDB
import types.dao.ContactTable


/**
 * This function retrieves all available ContactPartners from the database.
 * By design there will never be a sufficiently large amount of contact partners to warrant some kind of pagination.
 * @receiver Repository<ContactPartner>
 * @return Sequence<ContactPartner>
 */
suspend fun ReadDB<ContactPartner>.getAllContactPartners() =
    getByQuery(ContactTable.selectAll())

