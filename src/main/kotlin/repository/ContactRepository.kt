package repository

import arrow.core.Option
import arrow.core.Valid
import kotlinx.coroutines.Dispatchers
import model.ContactPartner
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.dsl.module
import repository.dao.ContactTable
import repository.extensions.paginate

val contactModule = module {

    single { ContactReader }
    single { ContactWriter }
    single { ContactRepository }
}

typealias ContactIndex = PrimaryKey<ContactPartner>
typealias ValidContact = Valid<ContactPartner>


object ContactReader : Reader<ContactPartner> {
    override suspend fun byId(id: PrimaryKey<ContactPartner>): Option<ContactPartner> =
        newSuspendedTransaction(Dispatchers.IO) {
            ContactTable.select { ContactTable.id eq id.key }
                .singleOrNull()
                .asOption(ResultRow::toContactPartner)
        }

    override suspend fun byQuery(query: Query, limit: Int?, offset: Int?): QueryResult<ContactPartner> =
        (ContactRepository.countOf(query) to
                newSuspendedTransaction(Dispatchers.IO) {
                    query
                        .paginate(limit, offset)
                        .map { it.toContactPartner() }
                }
                ).let { (count, seq) -> QueryResult(count, seq) }


    override suspend fun countOf(query: Query): Int = newSuspendedTransaction(Dispatchers.IO) { query.count() }

}

object ContactWriter : Writer<ContactPartner> {

    override suspend fun update(entry: ValidContact): Result<ContactIndex> = safeTransactionIO(ContactTable) {
        val (contact) = entry
        val (key) = contact.id
        update({ id eq key }) { contact.toStatement(it) }
    }.map(::keyOf)


    override suspend fun create(entry: ValidContact): Result<ContactPartner> = safeTransactionIO(ContactTable) {
        val (contact) = entry
        insert { contact.toStatement(it) } get id
    }.map { (key) -> entry.a.copy(id = keyOf(key)) }


    override suspend fun delete(id: PrimaryKey<ContactPartner>): Result<ContactIndex> =
        safeTransactionIO(ContactTable) {
            deleteWhere { ContactTable.id eq id.key }
        }.map(::keyOf)


}

object ContactRepository :
    Reader<ContactPartner> by ContactReader,
    Writer<ContactPartner> by ContactWriter,
    Repository<ContactPartner>


internal fun ResultRow.toContactPartner(): ContactPartner = ContactPartner(
    id = keyOf(this[ContactTable.id].value),
    surname = this[ContactTable.firstName],
    lastname = this[ContactTable.lastName],
    phoneNumber = this[ContactTable.phoneNumber],
    url = this[ContactTable.url]
)

private fun ContactPartner.toStatement(statement: UpdateBuilder<Int>) =
    statement.run {
        this[ContactTable.firstName] = surname
        this[ContactTable.lastName] = lastname
        this[ContactTable.phoneNumber] = phoneNumber
        this[ContactTable.url] = url
    }