package service

class MissingIdError : Throwable()

sealed class Id<out T> {
    abstract val id: Int
}

object Uninitialized : Id<Nothing>() {
    override val id: Int
        get() = throw MissingIdError()
}

data class PrimaryKey<T>(override val id: Int) : Id<T>()


fun <T> Int.id() = PrimaryKey<T>(this)

