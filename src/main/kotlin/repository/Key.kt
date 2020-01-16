package repository


inline class PrimaryKey<T>(val key: Int) {

    operator fun component1() = key
}

fun <T> keyOf(key: Int) = PrimaryKey<T>(key)