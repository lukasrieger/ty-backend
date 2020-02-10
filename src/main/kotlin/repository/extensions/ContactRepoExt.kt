package repository.extensions

import kotlinx.coroutines.Dispatchers
import model.ContactPartner
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import repository.Repository
import repository.dao.ContactTable
import repository.toContactPartner


/**
 * This function retrieves all available ContactPartners from the database.
 * By design there will never be sufficiently large amounts of contact partners to warrant some kind of pagination.
 * @receiver Repository<ContactPartner>
 * @return Sequence<ContactPartner>
 */
suspend fun Repository<ContactPartner>.getContactPartners(): Sequence<ContactPartner> =
    newSuspendedTransaction(Dispatchers.IO) {
        ContactTable.selectAll()
            .mapNotNull { it.toContactPartner() }
    }.asSequence()

