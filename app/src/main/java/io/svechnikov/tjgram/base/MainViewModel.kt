package io.svechnikov.tjgram.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject

class MainViewModel @Inject constructor(): ViewModel() {

    private val eventMutable = MutableLiveData<MainEvent?>()
    val event: LiveData<MainEvent?> = eventMutable

    private val bottomBarVisibilityMutable = MutableLiveData<Boolean>()
    val bottomBarVisibility: LiveData<Boolean> = bottomBarVisibilityMutable

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