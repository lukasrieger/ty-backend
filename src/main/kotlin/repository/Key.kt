package repository


sealed class PrimaryKey<out T>(val key: Int) {
    operator fun component1() = key
}

internal object None : PrimaryKey<Nothing>(-1)

class Key<out T>(k: Int) : PrimaryKey<T>(key = k)


fun <T> keyOf(key: Int) = Key<T>(key)