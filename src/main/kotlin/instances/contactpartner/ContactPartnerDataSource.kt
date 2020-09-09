package instances.contactpartner

import arrow.fx.coroutines.parTraverse
import model.ContactPartner
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import service.DataSource
import service.Id
import service.dao.ContactTable
import service.id
import service.transactionEffect


private fun ContactPartner.toStatement(statement: UpdateBuilder<Int>) =
    statement.run {
        this[ContactTable.firstName] = surname
        this[ContactTable.lastName] = lastName
        this[ContactTable.phoneNumber] = phoneNumber
        this[ContactTable.url] = url
    }


internal object ContactPartnerDataSource : DataSource<ContactPartner> {

    override suspend fun get(id: Id<ContactPartner>): ContactPartner? =
        transactionEffect(ContactTable) {
            select { ContactTable.id eq id.id }
                .singleOrNull()
                ?.toContactPartner()
        }

    override suspend fun get(query: Query, limit: Int?, offset: Long?): List<ContactPartner> =
        transactionEffect(ContactTable) {
            query.parTraverse { it.toContactPartner() }
        }

    override suspend fun count(query: Query): Long =
        transactionEffect(ContactTable) { query.count() }

    override suspend fun update(value: ContactPartner) =
        transactionEffect(ContactTable) {
            update({ id eq value.id?.id }) { value.toStatement(it) }
            Unit
        }

    override suspend fun create(value: ContactPartner): ContactPartner =
        transactionEffect(ContactTable) {
            val id = insert { value.toStatement(it) } get id
            value.copy(id = id.value.id())
        }

    override suspend fun delete(id: Id<ContactPartner>) =
        transactionEffect(ContactTable) {
            deleteWhere { ContactTable.id eq id.id }
            Unit
        }

    private fun ResultRow.toContactPartner(): ContactPartner = ContactPartner(
        id = this[ContactTable.id].value.id(),
        surname = this[ContactTable.firstName],
        lastName = this[ContactTable.lastName],
        phoneNumber = this[ContactTable.phoneNumber],
        url = this[ContactTable.url]
    )

}