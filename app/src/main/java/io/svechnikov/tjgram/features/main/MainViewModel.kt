package io.svechnikov.tjgram.features.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import io.svechnikov.tjgram.base.SingleLiveEvent
import io.svechnikov.tjgram.base.data.WebSocketMessage
import io.svechnikov.tjgram.base.network.socketmessages.WebSocketMessages
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val socketMessages: WebSocketMessages,
    private val updateLikes: UpdateLikes
): ViewModel() {

    private val eventMutable = SingleLiveEvent<MainEvent>()
    val event: LiveData<MainEvent> = eventMutable

    private val bottomBarVisibilityMutable = MutableLiveData<Boolean>()
    val bottomBarVisibility: LiveData<Boolean> = bottomBarVisibilityMutable

    private val toolBarScrollableMutable = MutableLiveData<Boolean>()
    val toolBarScrollable: LiveData<Boolean> = toolBarScrollableMutable

    private val socketMessagesObserver: Observer<in WebSocketMessage> = Observer {message ->
        if (message.type == "content voted") {
            updateLikes(message) {}
        }
    }

    init {
        socketMessages.observeForever(socketMessagesObserver)
    }

    override fun onCleared() {
        super.onCleared()

        socketMessages.removeObserver(socketMessagesObserver)
        updateLikes.cancel()
    }

    fun setEvent(event: MainEvent) {
        eventMutable.value = event
    }

    fun setBottomBarVisibility(visibility: Boolean) {
        bottomBarVisibilityMutable.value = visibility
    }

    fun setToolbarScrollable(scrollable: Boolean) {
        toolBarScrollableMutable.value = scrollable
    }
}

sealed class MainEvent {
    data class ShowMessage(val message: String) : MainEvent()
}