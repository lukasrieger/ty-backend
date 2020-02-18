package repository.extensions

import kotlinx.coroutines.Dispatchers
import model.ContactPartner
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import repository.Reader
import repository.dao.ContactTable
import repository.toContactPartner


/**
 * This function retrieves all available ContactPartners from the database.
 * By design there will never be a sufficiently large amount of contact partners to warrant some kind of pagination.
 * @receiver Repository<ContactPartner>
 * @return Sequence<ContactPartner>
 */
suspend fun Reader<ContactPartner>.getContactPartners(): Sequence<ContactPartner> =
    newSuspendedTransaction(Dispatchers.IO) {
        ContactTable.selectAll()
            .mapNotNull { it.toContactPartner() }
    }.asSequence()

