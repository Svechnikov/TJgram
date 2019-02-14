package io.svechnikov.tjgram.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import io.svechnikov.tjgram.base.data.WebSocketMessage
import io.svechnikov.tjgram.base.db.BaseDatabase
import io.svechnikov.tjgram.base.network.socketmessages.WebSocketMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class MainViewModel @Inject constructor(
    private val socketMessages: WebSocketMessages,
    private val db: BaseDatabase
): ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private var job: Job? = null

    private val eventMutable = MutableLiveData<MainEvent?>()
    val event: LiveData<MainEvent?> = eventMutable

    private val bottomBarVisibilityMutable = MutableLiveData<Boolean>()
    val bottomBarVisibility: LiveData<Boolean> = bottomBarVisibilityMutable

    private val socketMessagesObserver: Observer<in WebSocketMessage> = Observer {message ->
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

    init {
        socketMessages.observeForever(socketMessagesObserver)
    }

    override fun onCleared() {
        super.onCleared()

        socketMessages.removeObserver(socketMessagesObserver)
        job?.cancel()
    }

    fun eventHandled() {
        eventMutable.value = null
    }

    fun setEvent(event: MainEvent) {
        eventMutable.value = event
    }

    fun setBottomBarVisibility(visibility: Boolean) {
        bottomBarVisibilityMutable.value = visibility
    }
}

sealed class MainEvent {
    data class ShowMessage(val message: String) : MainEvent()
}