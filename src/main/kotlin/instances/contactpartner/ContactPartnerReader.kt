package instances.contactpartner

import arrow.core.Nel
import model.ContactPartner
import model.DatabaseError
import service.*

private val contactPartnerErrorHandler =
    object : ErrorHandler<ContactPartnerValidationError, DatabaseError, ContactPartner> {
        override fun notFound(id: Id<ContactPartner>): DatabaseError =
            DatabaseError.NotFound(id)

        override fun handle(err: Throwable): DatabaseError =
            DatabaseError.UnknownError(err)

        override fun validationFailed(errors: Nel<ContactPartnerValidationError>): DatabaseError =
            DatabaseError.ValidationFailed(errors)

        override fun missingId(value: ContactPartner): DatabaseError =
            DatabaseError.UninitializedValue(value)

    }

private val ContactPartnerReaderSyntax = object :
    ReaderSyntax<ContactPartnerValidationError, DatabaseError, ContactPartner> {

    override val dataSource: DataSource<ContactPartner> = ContactPartnerDataSource
    override val errorHandler: ErrorHandler<ContactPartnerValidationError, DatabaseError, ContactPartner> =
        contactPartnerErrorHandler

}

internal object DefaultContactPartnerReader :
    Reader.Default<ContactPartnerValidationError, DatabaseError, ContactPartner>,
    ReaderSyntax<ContactPartnerValidationError, DatabaseError, ContactPartner>
    by ContactPartnerReaderSyntax