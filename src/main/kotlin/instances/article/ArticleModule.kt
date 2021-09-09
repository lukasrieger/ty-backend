package instances.article

import model.Article
import types.DatabaseContext
import types.ReadDB
import types.WriteDB
import types.dao.ArticlesTable
import validation.Validate


object ArticleService :
    ReadDB<Article> by ArticleReader,
    WriteDB<Article> by ArticleWriter {

    override val context: DatabaseContext = DatabaseContext(ArticlesTable)
    val validator: Validate<ArticleValidationError, Article> = ArticleValidate
}
