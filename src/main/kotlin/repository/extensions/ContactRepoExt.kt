package repository.extensions

import arrow.core.Either
import kotlinx.coroutines.Dispatchers
import model.ContactPartner
import model.extensions.fromResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import repository.Reader
import repository.dao.ContactTable


/**
 * This function retrieves all available ContactPartners from the database.
 * By design there will never be a sufficiently large amount of contact partners to warrant some kind of pagination.
 * @receiver Repository<ContactPartner>
 * @return Sequence<ContactPartner>
 */
suspend fun Reader<ContactPartner>.getContactPartners(): Either<Throwable, Sequence<ContactPartner>> =
    Either.catch {
        with(ContactPartner.fromResultRow) {
            newSuspendedTransaction(Dispatchers.IO) {
                ContactTable.selectAll()
                    .mapNotNull { it.coerce() }
            }.asSequence()
        }
    }



