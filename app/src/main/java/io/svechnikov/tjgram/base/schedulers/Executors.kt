package io.svechnikov.tjgram.base.schedulers

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject

class Executors @Inject constructor() {
    fun newSingleThreadExecutor(): Executor {
        return Executors.newSingleThreadExecutor()
    }
}