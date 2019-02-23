package io.svechnikov.tjgram.features.addpost.selectimage.takephoto

sealed class TakePhotoState {
    object PermissionsRejected : TakePhotoState()
    object ShowViewfinder : TakePhotoState()
    object HideViewfinder : TakePhotoState()
    object ShowCameraError : TakePhotoState()
    object ProcessingPhoto : TakePhotoState()
}