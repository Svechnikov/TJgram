package io.svechnikov.tjgram.features.auth.selectmethod

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject

class SelectAuthMethodViewModel @Inject constructor(
): ViewModel() {

    private val eventMutable = MutableLiveData<SelectAuthMethodEvent?>()

    val event: LiveData<SelectAuthMethodEvent?> = eventMutable

    fun authWithQr() {
        eventMutable.value = SelectAuthMethodEvent.NavigateToQr
    }

    fun eventHandled() {
        eventMutable.value = null
    }
}

sealed class SelectAuthMethodEvent {
    object NavigateToQr : SelectAuthMethodEvent()
}