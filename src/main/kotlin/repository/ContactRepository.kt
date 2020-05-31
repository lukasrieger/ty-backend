package repository

import arrow.core.Either
import arrow.core.Valid
import arrow.core.extensions.either.monad.flatten
import arrow.fx.IO
import arrow.fx.extensions.fx
import kotlinx.coroutines.Dispatchers
import model.ContactPartner
import model.extensions.fromResultRow
import model.id
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
    override suspend fun byId(id: PrimaryKey<ContactPartner>): Either<Throwable, ContactPartner?> =
        IO.fx<Throwable, ContactPartner?> {
            with(ContactPartner.fromResultRow) {
                newSuspendedTransaction(Dispatchers.IO) {
                    ContactTable.select { ContactTable.id eq id.key }
                        .singleOrNull()
                        ?.coerce()
                }
            }
        }.suspended()


    override suspend fun byQuery(
        query: Query,
        limit: Int?,
        offset: Long?
    ): Either<Throwable, QueryResult<ContactPartner>> =
        Either.catch {
            with(ContactPartner.fromResultRow) {
                val queryResult = query.paginate(limit, offset).map { it.coerce() }
                countOf(query).map { count ->
                    QueryResult(count, queryResult)
                }

            }
        }.flatten()


    override suspend fun countOf(query: Query): Either<Throwable, Long> =
        Either.catch { newSuspendedTransaction(Dispatchers.IO) { query.count() } }

}

object ContactWriter : Writer<ContactPartner> {

    override suspend fun update(entry: ValidContact): Either<Throwable, ValidContact> =
        transactionContext(ContactTable) {
            val (contact) = entry
            val (key) = contact.id
            update({ id eq key }) { contact.toStatement(it) }
        }.map { entry }


    override suspend fun create(entry: ValidContact): Either<Throwable, ValidContact> =
        transactionContext(ContactTable) {
            val (contact) = entry
            insert { contact.toStatement(it) } get id
        }.map { (key) -> Valid(ContactPartner.id.set(entry.a, keyOf(key))) }


    override suspend fun delete(id: PrimaryKey<ContactPartner>): Either<Throwable, ContactIndex> =
        transactionContext(ContactTable) {
            deleteWhere { ContactTable.id eq id.key }
        }.map(::keyOf)
}

object ContactRepository :
    Reader<ContactPartner> by ContactReader,
    Writer<ContactPartner> by ContactWriter,
    Repository<ContactPartner>


private fun ContactPartner.toStatement(statement: UpdateBuilder<Int>) =
    statement.run {
        this[ContactTable.firstName] = surname
        this[ContactTable.lastName] = lastName
        this[ContactTable.phoneNumber] = phoneNumber
        this[ContactTable.url] = url
    }