package repository

import arrow.core.Option
import kotlinx.coroutines.Dispatchers
import model.ContactPartner
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import repository.dao.ContactTable


internal typealias ContactIndex = PrimaryKey<ContactPartner>

object ContactReader : ReadableRepository<ContactPartner> {
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

object ContactWriter : WritableRepository<ContactPartner> {

    override suspend fun update(entry: ContactPartner): Result<ContactIndex> =
        newSuspendedTransaction(Dispatchers.IO) {
            val (key) = entry.id
            ContactTable.runCatching {
                update({ ContactTable.id eq key }) { entry.toStatement(it) }
            }.mapCatching {
                keyOf<ContactPartner>(it)
            }
        }.foldEither()


    override suspend fun create(entry: ContactPartner): Result<ContactPartner> =
        newSuspendedTransaction(Dispatchers.IO) {
            ContactTable.runCatching {
                insert {
                    entry.toStatement(it)
                } get ContactTable.id
            }.mapCatching { (key) ->
                entry.copy(id = keyOf(key))
            }
        }.foldEither()

    override suspend fun delete(id: PrimaryKey<ContactPartner>): Result<ContactIndex> =
        newSuspendedTransaction(Dispatchers.IO) {
            ContactTable.runCatching {
                deleteWhere {
                    ContactTable.id eq id.key
                }
            }.mapCatching {
                keyOf<ContactPartner>(it)
            }
        }.foldEither()


}

object ContactRepository :
    ReadableRepository<ContactPartner> by ContactReader,
    WritableRepository<ContactPartner> by ContactWriter,
    Repository<ContactPartner>


internal fun ResultRow.toContactPartner() =
    ContactPartner(
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