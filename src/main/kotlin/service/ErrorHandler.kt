package service

import arrow.core.Nel

interface ErrorHandler<V, E, T> {
    fun notFound(id: Id<T>): E

    fun handle(err: Throwable): E

    fun validationFailed(errors: Nel<V>): E

    fun missingId(value: T): E
}