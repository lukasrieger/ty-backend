package validation

import model.Article
import kotlin.reflect.KProperty1

sealed class ArticleValidationError {

    data class BlankField<T>(val field: KProperty1<Article, T>) : ArticleValidationError()

    object InvalidApplicationDate : ArticleValidationError()
}