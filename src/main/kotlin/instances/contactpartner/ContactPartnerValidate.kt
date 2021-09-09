package instances.contactpartner

import arrow.core.Validated.Companion.validNel
import model.ContactPartner
import validation.Validate

val ContactPartnerValidate = Validate<Nothing, ContactPartner>(::validNel)