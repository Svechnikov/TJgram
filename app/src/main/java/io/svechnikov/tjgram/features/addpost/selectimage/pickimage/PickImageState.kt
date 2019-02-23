package io.svechnikov.tjgram.features.addpost.selectimage.pickimage

sealed class PickImageState {
    object Idle: PickImageState()
    object Loading : PickImageState()
    object Loaded : PickImageState()
    object Refreshing : PickImageState()
    object PermissionsRejected : PickImageState()
}