package io.svechnikov.tjgram.features.addpost.selectimage

sealed class SelectImageEvent {
    data class OpenSendImage(val imageId: Int) : SelectImageEvent()
    object GoBack : SelectImageEvent()
    object NavigateToAuth : SelectImageEvent()
    data class ShowError(val error: String) : SelectImageEvent()
    object RequestCameraPermissions : SelectImageEvent()
    object RequestGalleryPermissions : SelectImageEvent()
}