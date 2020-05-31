package repository.extensions

import arrow.Kind
import model.ContactPartner
import org.jetbrains.exposed.sql.selectAll
import repository.Reader
import repository.concurrent
import repository.dao.ContactTable


/**
 * This function retrieves all available ContactPartners from the database.
 * By design there will never be a sufficiently large amount of contact partners to warrant some kind of pagination.
 * @receiver Repository<ContactPartner>
 * @return Sequence<ContactPartner>
 */
fun <F> Reader<F, ContactPartner>.getContactPartners(): Kind<F, List<ContactPartner>> =
    concurrent {
        val contacts = !byQuery(ContactTable.selectAll())
        contacts.result.toList()
    }





