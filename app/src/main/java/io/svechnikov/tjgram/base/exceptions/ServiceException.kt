package io.svechnikov.tjgram.base.exceptions

class ServiceException(val code: Int, val serviceMessage: String) : Throwable()