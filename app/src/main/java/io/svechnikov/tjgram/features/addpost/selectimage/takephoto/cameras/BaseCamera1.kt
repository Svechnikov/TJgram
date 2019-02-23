package io.svechnikov.tjgram.features.addpost.selectimage.takephoto.cameras

import android.graphics.ImageFormat
import android.hardware.Camera
import android.view.Display
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import timber.log.Timber

// todo потестить на разных устройствах
@Suppress("deprecation")
class BaseCamera1 : BaseCamera {

    private var camera: Camera? = null
    private var maxWidth = -1
    private var maxHeight = -1
    private lateinit var preview: SurfaceView
    private lateinit var onErrorCallback: (Throwable) -> Unit
    private lateinit var onPhotoTakenCallback: (ByteArray) -> Unit
    private lateinit var display: Display

    private val holderCallback = object: SurfaceHolder.Callback {
        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            Timber.i("surfaceChanged")
        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            Timber.i("surfaceDestroyed")
            close()
        }

        override fun surfaceCreated(holder: SurfaceHolder?) {
            Timber.i("surfaceCreated")
            startPreview()
        }
    }

    private val pictureCallback = Camera.PictureCallback { data, _ ->
        onPhotoTakenCallback(data)
    }

    override fun open() {
        try {
            camera = Camera.open(0).apply {
                val autofocusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                if (parameters.supportedFocusModes.contains(autofocusMode)) {
                    parameters.focusMode = autofocusMode
                }
                var pictureSize: Camera.Size? = null
                val sizes = parameters.supportedPictureSizes
                sizes.sortByDescending {
                    it.width * it.height
                }
                for (size in sizes) {
                    if (size.width <= maxWidth && size.height <= maxHeight) {
                        pictureSize = size
                        break
                    }
                }
                pictureSize?.let {
                    parameters.pictureFormat = ImageFormat.JPEG
                    parameters.setPictureSize(pictureSize.width, pictureSize.height)
                    parameters.setPreviewSize(pictureSize.width, pictureSize.height)
                } ?: throw IllegalStateException()
            }

            if (preview.holder?.isCreating == false) {
                startPreview()
            }
            preview.holder?.apply {
                addCallback(holderCallback)
            }
        }
        catch (e: Throwable) {
            close()
            onErrorCallback(e)
        }
    }

    private fun startPreview() {
        try {
            camera?.apply {
                val info = Camera.CameraInfo()
                Camera.getCameraInfo(0, info)
                val degrees = when(display.rotation) {
                    Surface.ROTATION_0 -> 90
                    Surface.ROTATION_90 -> 0
                    Surface.ROTATION_180 -> 270
                    Surface.ROTATION_270 -> 180
                    else -> throw IllegalStateException()
                }
                setDisplayOrientation(degrees)
                setPreviewDisplay(preview.holder)
                startPreview()
            }
        }
        catch (e: Throwable) {
            close()
            onErrorCallback(e)
        }
    }

    override fun close() {
        camera?.apply {
            preview.holder?.removeCallback(holderCallback)
            camera?.release()
            camera = null
            Timber.i("close")
        }
    }

    override fun takePhoto() {
        camera?.takePicture(null, null, pictureCallback)
    }

    override fun setMaxWidth(width: Int) {
        maxWidth = width
    }

    override fun setMaxHeight(height: Int) {
        maxHeight = height
    }

    override fun setPreview(preview: Any) {
        this.preview = preview as SurfaceView
    }

    override fun setDisplay(display: Display) {
        this.display = display
    }

    override fun setOnErrorCallback(callback: (Throwable) -> Unit) {
        onErrorCallback = callback
    }

    override fun setOnPhotoTakenCallback(callback: (ByteArray) -> Unit) {
        onPhotoTakenCallback = callback
    }
}