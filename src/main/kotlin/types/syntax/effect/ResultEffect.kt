package types.syntax.effect

import arrow.continuations.Effect
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.coroutines.RestrictsSuspension

fun interface ResultEffect<R> : Effect<Result<R>> {
    suspend fun <B> Result<B>.bind(): B =
        fold(onSuccess = { it }, onFailure = { control().shift(Result.failure(it)) })

    /**
     * Ensure check if the [value] is `true`,
     * and if it is it allows the `result { }` binding to continue.
     * In case it is `false`, then it short-circuits the binding and returns
     * the provided value by [onFailure] inside a failed [Result].
     *
     * ```kotlin:ank:playground
     * import arrow.core.computations.result
     *
     * //sampleStart
     * suspend fun main() {
     *   result<Int> {
     *     ensure(true) { Throwable("Assertion 1 failed.") }
     *     println("ensure(true) passes")
     *     ensure(false) { Throwable("Assertion 2 failed.") }
     *     1
     *   }
     * //sampleEnd
     *   .let(::println)
     * }
     * // println: "ensure(true) passes"
     * // res: Failure(java.lang.Throwable: Assertion 2 failed.)
     * ```
     */
    suspend fun ensure(value: Boolean, onFailure: () -> Throwable) {
        if (value) Unit else Result.failure<R>(onFailure()).bind()
    }
}

/**
 * Ensures that [value] is not null.
 * When the value is not null, then it will be returned as non null and the check value is now smart-checked to non-null.
 * Otherwise, if the [value] is null then the [result] binding will short-circuit with [onFailure].
 *
 * ```kotlin:ank
 * import arrow.core.computations.either
 * import arrow.core.computations.ensureNotNull
 *
 * //sampleStart
 * suspend fun main() {
 *   result<Int> {
 *     val x: Int? = 1
 *     ensureNotNull(x) { Throwable("passes") }
 *     println(x)
 *     ensureNotNull(null) { Throwable("failed") }
 *   }
 * //sampleEnd
 *   .let(::println)
 * }
 * // println: "1"
 * // res: Failure(java.lang.Throwable: failed)
 * ```
 */

@OptIn(ExperimentalContracts::class)
suspend fun <R : Any> ResultEffect<*>.ensureNotNull(value: R?, onFailure: () -> Throwable): R {
    contract {
        returns() implies (value != null)
    }

    return value ?: Result.failure<R>(onFailure()).bind()
}

@RestrictsSuspension
fun interface RestrictedResultEffect<R> : ResultEffect<R>

@Suppress("ClassName")
object result {
    inline fun <R> eager(crossinline c: suspend RestrictedResultEffect<*>.() -> R): Result<R> =
        Effect.restricted(eff = { RestrictedResultEffect { it } }, f = c, just = { Result.success(it) })

    suspend inline operator fun <R> invoke(crossinline c: suspend ResultEffect<*>.() -> R): Result<R> =
        Effect.suspended(eff = { ResultEffect { it } }, f = c, just = { Result.success(it) })
}

