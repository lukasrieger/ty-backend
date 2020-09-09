package instances.article

import model.Article
import model.ContactPartner
import model.DatabaseError
import service.Reader
import service.Service
import service.ServiceSyntax
import validation.Validator


private val ArticleServiceSyntax =
    object : ServiceSyntax<ArticleValidationError, DatabaseError, Article> {
        override val Service<ArticleValidationError, DatabaseError, Article>.validator:
                Validator<ArticleValidationError, Article>
            get() = DefaultArticleValidator(this)
    }


internal class DefaultArticleService(contactReader: Reader<*, *, ContactPartner>) :
    ServiceSyntax<ArticleValidationError, DatabaseError, Article> by ArticleServiceSyntax,
    Service.Default<ArticleValidationError, DatabaseError, Article>,
    Reader<ArticleValidationError, DatabaseError, Article> by DefaultArticleReader(contactReader)