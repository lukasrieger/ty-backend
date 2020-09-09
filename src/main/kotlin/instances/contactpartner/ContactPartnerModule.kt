package instances.contactpartner


import model.ContactPartner
import model.DatabaseError
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton
import service.Reader
import service.Service



val defaultContactPartnerModule = DI.Module("ContactPartnerModule", false) {
    bind<Reader<ContactPartnerValidationError, DatabaseError, ContactPartner>>() with singleton {
        DefaultContactPartnerReader
    }

    bind<Service<ContactPartnerValidationError, DatabaseError, ContactPartner>>() with singleton {
        DefaultContactPartnerService
    }
}