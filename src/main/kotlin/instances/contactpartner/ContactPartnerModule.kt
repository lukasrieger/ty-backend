package instances.contactpartner

import model.ContactPartner
import types.DatabaseContext
import types.ReadDB
import types.WriteDB
import types.dao.ArticlesTable
import validation.Validate


object ContactPartnerService :
    ReadDB<ContactPartner> by ContactPartnerReader,
    WriteDB<ContactPartner> by ContactPartnerWriter {

    override val context: DatabaseContext = DatabaseContext(ArticlesTable)
    val validator: Validate<*, ContactPartner> = ContactPartnerValidate
}