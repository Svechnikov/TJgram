package io.svechnikov.tjgram.features.timeline.parent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.svechnikov.tjgram.base.SingleLiveEvent
import io.svechnikov.tjgram.base.db.entities.Post
import javax.inject.Inject

class TimelineParentViewModel @Inject constructor(): ViewModel() {

    private val stateMutable = MutableLiveData<TimelineParentState>()
    val state: LiveData<TimelineParentState> = stateMutable

    private val eventMutable = SingleLiveEvent<TimelineParentEvent>()
    val event: LiveData<TimelineParentEvent> = eventMutable

    fun selectSorting(sorting: Post.Sorting) {
        state.value.let {
            if (it !is TimelineParentState.ShowPage || it.sorting != sorting) {
                stateMutable.value = TimelineParentState.ShowPage(sorting)
            }
        }
    }

    fun onNavigateToAuth() {
        eventMutable.value = TimelineParentEvent.NavigateToAuth
    }
}