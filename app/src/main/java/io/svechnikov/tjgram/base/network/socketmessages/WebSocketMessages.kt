package io.svechnikov.tjgram.base.network.socketmessages

import androidx.lifecycle.LiveData
import com.google.gson.Gson
import io.svechnikov.tjgram.base.data.WebSocketMessage
import kotlinx.coroutines.*
import okhttp3.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class WebSocketMessages @Inject constructor(
    private val httpClient: OkHttpClient,
    private val gson: Gson,
    @Named("webSocketUrl") private val url: String) : CoroutineScope,
    LiveData<WebSocketMessage>() {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private var socket: WebSocket? = null
    private var openingOrOpen = false

    private var retryJob: Job? = null

    private val listener = object: WebSocketListener() {

        override fun onOpen(webSocket: WebSocket,
                            response: Response) {
            super.onOpen(webSocket, response)

            Timber.i("onOpen")
        }

        override fun onFailure(webSocket: WebSocket,
                               t: Throwable,
                               response: Response?) {
            super.onFailure(webSocket, t, response)

            openingOrOpen = false

            Timber.i("onFailure")
            Timber.e(t)

            retryJob = launch {
                delay(1000)

                open()
            }
        }

        override fun onMessage(webSocket: WebSocket,
                               text: String) {
            super.onMessage(webSocket, text)

            val message = gson.fromJson(text, WebSocketMessage::class.java)

            Timber.i("onMessage: $message")

            postValue(message)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)

            openingOrOpen = false
        }
    }

    override fun onActive() {
        super.onActive()

        open()
    }

    override fun onInactive() {
        super.onInactive()

        close()
    }

    private fun close() {
        socket?.close(1000, "closing")
        retryJob?.cancel()
        openingOrOpen = false
    }

    private fun open() {
        if (openingOrOpen || !isActive) {
            return
        }
        openingOrOpen = true

        val request = Request.Builder()
            .url(url).build()

        socket = httpClient.newWebSocket(request, listener)
    }
}