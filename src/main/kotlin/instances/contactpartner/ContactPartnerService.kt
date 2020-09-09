package instances.contactpartner

import model.ContactPartner
import model.DatabaseError
import service.Reader
import service.Service
import service.ServiceSyntax
import validation.AbstractValidator
import validation.Validator

internal val contactPartnerValidator =
    object : AbstractValidator<ContactPartnerValidationError, ContactPartner>() {}

private val ContactPartnerServiceSyntax =
    object : ServiceSyntax<ContactPartnerValidationError, DatabaseError, ContactPartner> {
        override val Service<ContactPartnerValidationError, DatabaseError, ContactPartner>.validator:
                Validator<ContactPartnerValidationError, ContactPartner>
            get() = contactPartnerValidator
    }


internal object DefaultContactPartnerService :
    ServiceSyntax<ContactPartnerValidationError, DatabaseError, ContactPartner> by ContactPartnerServiceSyntax,
    Service.Default<ContactPartnerValidationError, DatabaseError, ContactPartner>,
    Reader<ContactPartnerValidationError, DatabaseError, ContactPartner> by DefaultContactPartnerReader