package instances.article

import arrow.core.Nel
import model.Article
import model.ContactPartner
import model.DatabaseError
import service.*

private val articleErrorHandler =
    object : ErrorHandler<ArticleValidationError, DatabaseError, Article> {

        override fun notFound(id: Id<Article>): DatabaseError = DatabaseError.NotFound(id)

        override fun handle(err: Throwable): DatabaseError =
            DatabaseError.UnknownError(err)

        override fun validationFailed(errors: Nel<ArticleValidationError>): DatabaseError =
            DatabaseError.ValidationFailed(errors)

    }

private class ArticleReaderSyntax(contactReader: Reader<*, *, ContactPartner>) :
    ReaderSyntax<ArticleValidationError, DatabaseError, Article> {
    override val dataSource: DataSource<Article> = ArticleDataSource(contactReader)
    override val errorHandler: ErrorHandler<ArticleValidationError, DatabaseError, Article> = articleErrorHandler

}

internal class DefaultArticleReader(contactReader: Reader<*, *, ContactPartner>) :
    Reader.Default<ArticleValidationError, DatabaseError, Article>,
    ReaderSyntax<ArticleValidationError, DatabaseError, Article> by ArticleReaderSyntax(contactReader)
