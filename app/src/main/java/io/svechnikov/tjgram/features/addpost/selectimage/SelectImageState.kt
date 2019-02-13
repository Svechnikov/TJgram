package io.svechnikov.tjgram.features.addpost.selectimage

sealed class SelectImageState {
    object Idle: SelectImageState()
    object Loading : SelectImageState()
    object Loaded : SelectImageState()
    object Refreshing : SelectImageState()
}