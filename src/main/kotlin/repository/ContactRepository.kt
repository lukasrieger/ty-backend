package repository

import arrow.Kind
import arrow.core.Valid
import arrow.fx.ForIO
import arrow.fx.IO
import arrow.fx.extensions.io.concurrent.concurrent
import arrow.fx.typeclasses.Concurrent
import arrow.typeclasses.ApplicativeError
import model.ContactPartner
import model.id
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import repository.dao.ContactTable
import repository.extensions.queryPaginate


typealias ContactIndex = PrimaryKey<ContactPartner>
typealias ValidContact = Valid<ContactPartner>


class ContactReader<F>(A: ApplicativeError<F, Throwable>) :
    Reader<F, ContactPartner>, ApplicativeError<F, Throwable> by A {

    @JvmName("nullableCoerce")
    private fun ResultRow?.coerce(): Kind<F, ContactPartner?> = TODO()
    private fun ResultRow.coerce(): Kind<F, ContactPartner> = TODO()

    override fun Concurrent<F>.byId(id: PrimaryKey<ContactPartner>): Kind<F, ContactPartner?> =
        fx.concurrent {
            !!transactionContext(ContactTable) {
                ContactTable.select { ContactTable.id eq id.key }
                    .singleOrNull()
                    .coerce()
            }
        }


    override fun Concurrent<F>.byQuery(query: Query, limit: Int?, offset: Long?): Kind<F, QueryResult<ContactPartner>> =
        fx.concurrent {
            val count = !countOf(query)
            val queryResult = queryPaginate(query, limit, offset).map { !it.coerce() }
            QueryResult(count, queryResult)
        }


    override fun Concurrent<F>.countOf(query: Query): Kind<F, Long> =
        fx.concurrent {
            !effect { newSuspendedTransaction { query.count() } }
        }

}

class ContactWriter<F>(A: ApplicativeError<F, Throwable>) :
    Writer<F, ContactPartner>, ApplicativeError<F, Throwable> by A {

    override fun Concurrent<F>.update(entry: Valid<ContactPartner>): Kind<F, Valid<ContactPartner>> =
        fx.concurrent {
            !transactionContext(ContactTable) {
                val (contact) = entry
                val (key) = contact.id
                update({ id eq key }) { contact.toStatement(it) }
            }

            entry
        }


    override fun Concurrent<F>.create(entry: ValidContact): Kind<F, Valid<ContactPartner>> =
        fx.concurrent {
            val id = !transactionContext(ContactTable) {
                val (contact) = entry
                insert { contact.toStatement(it) } get id
            }

            Valid(ContactPartner.id.set(entry.a, keyOf(id.value)))
        }


    override fun Concurrent<F>.delete(id: PrimaryKey<ContactPartner>): Kind<F, PrimaryKey<ContactPartner>> =
        fx.concurrent {
            !transactionContext(ContactTable) { deleteWhere { ContactTable.id eq id.key } }
            id
        }

}

class ContactRepository<F>(A: ApplicativeError<F, Throwable>) :
    Reader<F, ContactPartner> by ContactReader(A),
    Writer<F, ContactPartner> by ContactWriter(A),
    Repository<F, ContactPartner>


private fun ContactPartner.toStatement(statement: UpdateBuilder<Int>) =
    statement.run {
        this[ContactTable.firstName] = surname
        this[ContactTable.lastName] = lastName
        this[ContactTable.phoneNumber] = phoneNumber
        this[ContactTable.url] = url
    }


fun test(repo: ContactRepository<ForIO>) = with(repo) {

    val x = IO.concurrent().delete(keyOf(34))
}
