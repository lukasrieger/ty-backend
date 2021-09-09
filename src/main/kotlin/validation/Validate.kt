package validation

import arrow.core.*
import arrow.typeclasses.Semigroup


fun interface Validate<E, F> {
    companion object {
        fun <E, F> combine(vararg f: (F) -> Validated<E, F>): Validate<E, F> = Validate {
            f.map { it(this).mapLeft { nonEmptyListOf(it) } }
                .sequenceValidated(Semigroup.nonEmptyList())
                .fold(
                    fe = { it.invalid() },
                    fa = { this.valid() }
                )
        }
    }

    fun F.validate(): ValidatedNel<E, F>
}

operator fun <E, F> Validate<E, F>.invoke(value: F): ValidatedNel<E, F> = value.validate()
