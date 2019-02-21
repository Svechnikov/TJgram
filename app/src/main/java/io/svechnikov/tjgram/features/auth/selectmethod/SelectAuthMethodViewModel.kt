package io.svechnikov.tjgram.features.auth.selectmethod

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import io.svechnikov.tjgram.base.SingleLiveEvent
import javax.inject.Inject

class SelectAuthMethodViewModel @Inject constructor(
): ViewModel() {

    private val eventMutable = SingleLiveEvent<SelectAuthMethodEvent>()

    val event: LiveData<SelectAuthMethodEvent> = eventMutable

    fun authWithQr() {
        eventMutable.value = SelectAuthMethodEvent.NavigateToQr
    }
}

sealed class SelectAuthMethodEvent {
    object NavigateToQr : SelectAuthMethodEvent()
}