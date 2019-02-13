package io.svechnikov.tjgram.base

sealed class Either<out L, out R> {

    data class Error<out L>(val a: L) : Either<L, Nothing>()

    data class Value<out R>(val b: R) : Either<Nothing, R>()

    fun either(fnL: (L) -> Unit, fnR: (R) -> Unit) {
        when(this) {
            is Error -> fnL(a)
            is Value -> fnR(b)
        }
    }
}