package service.extensions


import model.ContactPartner
import org.jetbrains.exposed.sql.selectAll
import service.Reader
import service.dao.ContactTable


/**
 * This function retrieves all available ContactPartners from the database.
 * By design there will never be a sufficiently large amount of contact partners to warrant some kind of pagination.
 * @receiver Repository<ContactPartner>
 * @return Sequence<ContactPartner>
 */
suspend fun Reader<*, *, ContactPartner>.getAllContactPartners(): List<ContactPartner> =
    dataSource.get(ContactTable.selectAll())

