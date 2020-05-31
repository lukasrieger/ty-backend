package repository

import arrow.Kind
import arrow.core.Valid
import arrow.fx.typeclasses.Concurrent
import arrow.syntax.function.pipe
import model.ContactPartner
import model.id
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import repository.dao.ContactTable
import repository.extensions.queryPaginate
import validation.Validator


typealias ContactIndex = PrimaryKey<ContactPartner>
typealias ValidContact = Valid<ContactPartner>

@JvmName("nullableToContactPartner")
fun <F> Concurrent<F>.toContactPartner(resultRow: ResultRow?): Kind<F, ContactPartner?> =
    resultRow?.let { toContactPartner(it) } ?: just(null)

fun <F> Concurrent<F>.toContactPartner(resultRow: ResultRow): Kind<F, ContactPartner> =
    fx.concurrent {
        ContactPartner(
            id = keyOf(resultRow[ContactTable.id].value),
            surname = resultRow[ContactTable.firstName],
            lastName = resultRow[ContactTable.lastName],
            phoneNumber = resultRow[ContactTable.phoneNumber],
            url = resultRow[ContactTable.url]
        )
    }


class ContactReader<F>(override val runtime: Concurrent<F>) : Reader<F, ContactPartner> {


    override fun byId(id: PrimaryKey<ContactPartner>): Kind<F, ContactPartner?> =
        concurrent {
            !!transactionEffect(ContactTable) {
                ContactTable.select { ContactTable.id eq id.key }
                    .singleOrNull()
                    .pipe { toContactPartner<F>(it) }
            }
        }


    override fun byQuery(query: Query, limit: Int?, offset: Long?): Kind<F, QueryResult<ContactPartner>> =
        concurrent {
            val count = !countOf(query)
            val queryResult = queryPaginate(query, limit, offset).bind()
                .map { !toContactPartner<F>(it) }
            QueryResult(count, queryResult)
        }


    override fun countOf(query: Query): Kind<F, Long> =
        concurrent {
            !effect { newSuspendedTransaction { query.count() } }
        }

}

class ContactWriter<F>(override val runtime: Concurrent<F>) : Writer<F, ContactPartner> {

    override fun update(entry: Valid<ContactPartner>): Kind<F, Valid<ContactPartner>> =
        concurrent {
            !transactionEffect(ContactTable) {
                val (contact) = entry
                val (key) = contact.id
                update({ id eq key }) { contact.toStatement(it) }
            }

            entry
        }


    override fun create(entry: ValidContact): Kind<F, Valid<ContactPartner>> =
        concurrent {
            val id = !transactionEffect(ContactTable) {
                val (contact) = entry
                insert { contact.toStatement(it) } get id
            }

            Valid(ContactPartner.id.set(entry.a, keyOf(id.value)))
        }


    override fun delete(id: ContactIndex): Kind<F, PrimaryKey<ContactPartner>> =
        concurrent {
            !transactionEffect(ContactTable) { deleteWhere { ContactTable.id eq id.key } }
            id
        }

}

class ContactRepository<F>(
    override val runtime: Concurrent<F>,
    override val validator: Validator<F, *, ContactPartner>
) :
    Reader<F, ContactPartner> by ContactReader(runtime),
    Writer<F, ContactPartner> by ContactWriter(runtime),
    Repository<F, ContactPartner>


private fun ContactPartner.toStatement(statement: UpdateBuilder<Int>) =
    statement.run {
        this[ContactTable.firstName] = surname
        this[ContactTable.lastName] = lastName
        this[ContactTable.phoneNumber] = phoneNumber
        this[ContactTable.url] = url
    }
