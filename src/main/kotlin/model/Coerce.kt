package model


interface Coerce<A, B> {
    suspend fun A.coerce(): B
}