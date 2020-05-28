package repository

import arrow.Kind
import arrow.core.Valid
import arrow.fx.ForIO
import arrow.fx.typeclasses.Concurrent
import model.ContactPartner
import model.id
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import repository.dao.ContactTable
import repository.extensions.queryPaginate


typealias ContactIndex = PrimaryKey<ContactPartner>
typealias ValidContact = Valid<ContactPartner>


class ContactReader<F>(private val C: Concurrent<F>) : Reader<F, ContactPartner> {

    @JvmName("nullableCoerce")
    private fun ResultRow?.coerce(): Kind<F, ContactPartner?> = TODO()
    private fun ResultRow.coerce(): Kind<F, ContactPartner> = TODO()

    override fun byId(id: PrimaryKey<ContactPartner>): Kind<F, ContactPartner?> =
        C.fx.concurrent {
            !!transactionContext(ContactTable) {
                ContactTable.select { ContactTable.id eq id.key }
                    .singleOrNull()
                    .coerce()
            }
        }


    override fun byQuery(query: Query, limit: Int?, offset: Long?): Kind<F, QueryResult<ContactPartner>> =
        C.fx.concurrent {
            val count = !countOf(query)
            val queryResult = queryPaginate(query, limit, offset).map { !it.coerce() }
            QueryResult(count, queryResult)
        }


    override fun countOf(query: Query): Kind<F, Long> =
        C.fx.concurrent {
            !effect { newSuspendedTransaction { query.count() } }
        }

}

class ContactWriter<F>(private val C: Concurrent<F>) : Writer<F, ContactPartner> {

    override fun update(entry: Valid<ContactPartner>): Kind<F, Valid<ContactPartner>> =
        C.fx.concurrent {
            !transactionContext(ContactTable) {
                val (contact) = entry
                val (key) = contact.id
                update({ id eq key }) { contact.toStatement(it) }
            }

            entry
        }


    override fun create(entry: ValidContact): Kind<F, Valid<ContactPartner>> =
        C.fx.concurrent {
            val id = !transactionContext(ContactTable) {
                val (contact) = entry
                insert { contact.toStatement(it) } get id
            }

            Valid(ContactPartner.id.set(entry.a, keyOf(id.value)))
        }


    override fun delete(id: PrimaryKey<ContactPartner>): Kind<F, PrimaryKey<ContactPartner>> =
        C.fx.concurrent {
            !transactionContext(ContactTable) { deleteWhere { ContactTable.id eq id.key } }
            id
        }

}

class ContactRepository<F>(C: Concurrent<F>) :
    Reader<F, ContactPartner> by ContactReader(C),
    Writer<F, ContactPartner> by ContactWriter(C),
    Repository<F, ContactPartner>


private fun ContactPartner.toStatement(statement: UpdateBuilder<Int>) =
    statement.run {
        this[ContactTable.firstName] = surname
        this[ContactTable.lastName] = lastName
        this[ContactTable.phoneNumber] = phoneNumber
        this[ContactTable.url] = url
    }


fun test(repo: ContactRepository<ForIO>) {

    val x = repo.delete(keyOf(234))

}
