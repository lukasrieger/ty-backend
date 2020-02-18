package repository

import arrow.core.Either
import arrow.core.Option
import kotlinx.coroutines.Dispatchers
import model.ContactPartner
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.dsl.module
import repository.dao.ContactTable

val contactModule = module {

    single { ContactReader }
    single { ContactWriter }
    single { ContactRepository }
}

internal typealias ContactIndex = PrimaryKey<ContactPartner>

object ContactReader : Reader<ContactPartner> {
    override suspend fun byId(id: PrimaryKey<ContactPartner>): Option<ContactPartner> =
        newSuspendedTransaction(Dispatchers.IO) {
            val (key) = id
            ContactTable.select { ContactTable.id eq key }
                .singleOrNull()
        }.asOption(ResultRow::toContactPartner)

    override suspend fun byQuery(query: Query, limit: Int?, offset: Int?): QueryResult<ContactPartner> =
        (ContactRepository.countOf(query) to
                newSuspendedTransaction(Dispatchers.IO) {
                    query
                        .also { query ->
                            limit?.let {
                                offset?.let { query.limit(limit, offset) }
                                query.limit(limit)
                            }
                        }.map { it.toContactPartner() }
                }
                ).let { (count, seq) -> QueryResult(count, seq) }


    override suspend fun countOf(query: Query): Int = newSuspendedTransaction(Dispatchers.IO) {
        query.count()
    }

}

object ContactWriter : Writer<ContactPartner> {

    override suspend fun update(entry: ContactPartner): Result<ContactIndex> = Either.catch {
        newSuspendedTransaction(Dispatchers.IO) {
            val (key) = entry.id
            ContactTable.run {
                update({ id eq key }) { entry.toStatement(it) }
            }
        }
    }.map { keyOf<ContactPartner>(it) }


    override suspend fun create(entry: ContactPartner): Result<ContactPartner> = Either.catch {
        newSuspendedTransaction(Dispatchers.IO) {
            ContactTable.run {
                insert {
                    entry.toStatement(it)
                } get id
            }
        }
    }.map { (key) -> entry.copy(id = keyOf(key)) }


    override suspend fun delete(id: PrimaryKey<ContactPartner>): Result<ContactIndex> = Either.catch {
        newSuspendedTransaction(Dispatchers.IO) {
            ContactTable.run {
                deleteWhere {
                    ContactTable.id eq id.key
                }
            }
        }
    }.map { keyOf<ContactPartner>(it) }


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