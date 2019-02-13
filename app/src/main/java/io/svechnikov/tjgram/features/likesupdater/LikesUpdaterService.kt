package io.svechnikov.tjgram.features.likesupdater

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.Observer
import dagger.android.AndroidInjection
import io.svechnikov.tjgram.base.data.WebSocketMessage
import io.svechnikov.tjgram.base.db.BaseDatabase
import io.svechnikov.tjgram.base.network.socketmessages.WebSocketMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Если нет необходимости в фоновом сервисе, можно не использовать сервис
 * и вместо этого привязаться к жизненному циклу MainActivity
 */
class LikesUpdaterService : Service(),
    CoroutineScope,
    Observer<WebSocketMessage> {

    @Inject
    lateinit var socketMessages: WebSocketMessages

    @Inject
    lateinit var db: BaseDatabase

    private var job: Job? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    override fun onCreate() {
        super.onCreate()

        AndroidInjection.inject(this)
    }

    override fun onChanged(message: WebSocketMessage) {
        if (message.type == "content voted") {
            val id = message.contentId
            val count = message.count

            job = launch {
                var isLiked = 0

                if (db.user().user()?.userHash == message.userHash) {
                    isLiked = message.state
                }

                db.posts().updateLikes(id, count, isLiked)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        socketMessages.observeForever(this)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()

        socketMessages.removeObserver(this)
        job?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}