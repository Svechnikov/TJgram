package io.svechnikov.tjgram.features.timeline.parent

import io.svechnikov.tjgram.base.db.entities.Post

sealed class TimelineParentState {
    data class ShowPage(val sorting: Post.Sorting) : TimelineParentState()
}