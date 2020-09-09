package instances.article

import instances.contactpartner.ContactPartnerValidationError
import model.Article
import model.ContactPartner
import model.DatabaseError
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import service.Reader
import service.Service
import validation.Validator



val defaultArticleModule = DI.Module("ArticleModule", false) {
    bind<Reader<ArticleValidationError, DatabaseError, Article>>() with singleton {
        DefaultArticleReader(instance<Reader<ContactPartnerValidationError, DatabaseError, ContactPartner>>())
    }

    bind<Service<ArticleValidationError, DatabaseError, Article>>() with singleton {
        DefaultArticleService(instance<Reader<ContactPartnerValidationError, DatabaseError, ContactPartner>>())
    }

    bind<Validator<ArticleValidationError,Article>>() with singleton {
        DefaultArticleValidator(instance())
    }
}