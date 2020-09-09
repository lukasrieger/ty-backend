package service


inline class Id<T>(val id: Int)


fun <T> Int.id() = Id<T>(this)