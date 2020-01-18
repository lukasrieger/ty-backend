package repository

import arrow.core.None
import arrow.core.Option
import arrow.core.Right
import arrow.core.Some
import model.error.leftOf
import org.jetbrains.exposed.sql.ResultRow

internal fun <T> kotlin.Result<T>.foldEither(): Result<T> = fold(
    onSuccess = { Right(it) },
    onFailure = { leftOf(it) }
)

internal suspend inline fun <T> ResultRow?.asOption(crossinline builder: suspend ResultRow.() -> T): Option<T> =
    when (this) {
        null -> None
        else -> Some(this.builder())
    }

internal inline fun <T> ResultRow?.asOption(builder: ResultRow.() -> T): Option<T> = when (this) {
    null -> None
    else -> Some(this.builder())
}