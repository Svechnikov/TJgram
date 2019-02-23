package io.svechnikov.tjgram.features.timeline.parent

sealed class TimelineParentEvent {
    object NavigateToAuth : TimelineParentEvent()
}