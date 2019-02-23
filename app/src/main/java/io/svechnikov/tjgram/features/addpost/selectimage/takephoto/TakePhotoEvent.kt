package io.svechnikov.tjgram.features.addpost.selectimage.takephoto

sealed class TakePhotoEvent {
    data class Error(val message: String) : TakePhotoEvent()
    data class NavigateToSendImage(val imageId: Int) : TakePhotoEvent()
}