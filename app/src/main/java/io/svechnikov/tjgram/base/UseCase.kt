package io.svechnikov.tjgram.base

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

abstract class UseCase<in Params, out Type> : CoroutineScope {

    private var job: Job? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    protected abstract suspend fun run(params: Params): Type

    operator fun invoke(params: Params, callback: (Either<Throwable, Type>) -> Unit) {
        cancel()

        job = launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    Either.Value(run(params))
                }
                catch (e: Throwable) {
                    Either.Error(e)
                }
            }
            callback(result)
        }
    }

    fun cancel() {
        job?.cancel()
    }
}