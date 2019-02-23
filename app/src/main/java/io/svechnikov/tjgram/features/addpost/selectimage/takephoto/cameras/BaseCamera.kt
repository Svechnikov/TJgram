package io.svechnikov.tjgram.features.addpost.selectimage.takephoto.cameras

import android.view.Display

/**
 * Базовый интерфейс камеры для поддержки
 * устройств с API LEVEL как ниже 21 (Camera) так и выше (Camera2)
 */
interface BaseCamera {
    fun open()
    fun close()
    fun takePhoto()
    fun setMaxWidth(width: Int)
    fun setMaxHeight(height: Int)
    fun setPreview(preview: Any)
    fun setDisplay(display: Display)
    fun setOnErrorCallback(callback: (Throwable) -> Unit)
    fun setOnPhotoTakenCallback(callback: (ByteArray) -> Unit)
}