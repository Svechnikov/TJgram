package io.svechnikov.tjgram.features.addpost.selectimage

sealed class SelectImageState {
    object ShowPickImageScreen : SelectImageState()
    object ShowTakePhotoScreen : SelectImageState()
}