package repository

/**
 * This class denotes a primary key for some type [T] that is also present in the database.
 * This prevents accidental table indexing with the wrong kind of key.
 * Note that the only way to manually create an instance of this class is through the [keyOf] function, which is
 * internal to this module.
 * This is a deliberate design choice. Fronted code should never be able to create an arbitrary primary key.
 * @param out T
 * @property key Int
 * @constructor
 */
sealed class PrimaryKey<out T>(val key: Int) {
    operator fun component1() = key
}

/**
 * This represents the absence of a primary key.
 * This state is always temporary and only present for newly created articles that have not yet been written to the
 * database.
 * The integer value of -1 is arbitrary and carries no special meaning other than the meaning of [None] itself.
 * @see keyOf to construct an actual primary key that references an existing entry
 */
internal object None : PrimaryKey<Nothing>(-1)

/**
 * This class represents a primary key for some type [T].
 * The given integer value [key] is implicitly assumed to be positive.
 * @param out T
 * @constructor
 */
class Key<out T>(k: Int) : PrimaryKey<T>(key = k)


/**
 * Helper method for creating a [PrimaryKey].
 * This function should be preferred to directly instantiating a new [Key] object.
 * @param key Int
 * @return Key<T>
 */
fun <T> keyOf(key: Int) = Key<T>(key)