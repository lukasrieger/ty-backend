package instances.article

import model.Article
import model.Priority
import org.joda.time.DateTime
import service.Id
import kotlin.reflect.KProperty1


sealed class ArticleValidationError {

    data class InvalidApplicationDate(val date: DateTime) : ArticleValidationError()

    data class InvalidPriority(val priority: Priority) : ArticleValidationError()

    data class BlankField<T>(val field: KProperty1<Article, T>) : ArticleValidationError()

    data class MissingArticle(val id: Id<Article>) : ArticleValidationError()

    /**
     * This class describes an error, where a child article claims to have a parent article while the parent article
     * does not have a child article. (Or vice versa)
     * @property parent Article
     * @property child Article
     * @constructor
     */
    data class AsymmetricRelation(val parent: Article, val child: Article) : ArticleValidationError()

    /**
     * This class is used in case of misaligned parent-child relationships:
     * A child my claim to have a parent article even though the parent already has a different child article.
     * @property parent Article
     * @property child Article
     * @constructor
     */
    data class InvalidRelation(val parent: Article, val child: Article) : ArticleValidationError()
}


