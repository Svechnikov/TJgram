package io.svechnikov.tjgram.features.addpost.selectimage.takephoto.cameras

import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Display
import android.view.Surface
import android.view.TextureView
import io.svechnikov.tjgram.features.addpost.selectimage.takephoto.AutoFitTextureView
import timber.log.Timber
import java.io.IOException
import java.util.*

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@SuppressWarnings("MissingPermission")
class BaseCamera2(private val context: Context) : BaseCamera {

    companion object {
        /**
         * Max preview width that is guaranteed by Camera2 API
         */
        private const val MAX_PREVIEW_WIDTH = 1920

        /**
         * Max preview height that is guaranteed by Camera2 API
         */
        private const val MAX_PREVIEW_HEIGHT = 1080
    }

    enum class State {
        /**
         * Camera state: Showing camera preview.
         */
        PREVIEW,

        /**
         * Camera state: Waiting for the focus to be locked.
         */
        WAITING_LOCK,

        /**
         * Camera state: Waiting for the exposure to be precapture state.
         */
        WAITING_PRECAPTURE,

        /**
         * Camera state: Waiting for the exposure state to be something other than precapture.
         */
        WAITING_NON_PRECAPTURE,

        /**
         * Camera state: Picture was taken.
         */
        PICTURE_TAKEN,

        PICTURE_CAPTURING
    }

    private var maxWidth = 0
    private var maxHeight = 0
    private lateinit var preview: AutoFitTextureView
    private lateinit var onErrorCallback: (Throwable) -> Unit

    private val cameraManager: CameraManager = context
        .getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var openCameraRequested = false
    private lateinit var backgroundHandler: Handler
    private var backgroundThread: HandlerThread? = null
    private var imageReader: ImageReader? = null
    private lateinit var onPhotoTakenCallback: (ByteArray) -> Unit
    private lateinit var display: Display
    private lateinit var previewSize: Size
    private lateinit var photoSize: Size
    private var captureSession: CameraCaptureSession? = null
    private var cameraId: String? = null
    private var state = State.PREVIEW
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var previewRequest: CaptureRequest? = null
    private var sensorOrientation = 0

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            try {
                createCameraPreviewSession()
            }
            catch (e: Throwable) {
                close()
                onErrorCallback(e)
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            close()
            onErrorCallback(IOException("Error code $error"))
        }
    }

    private val textureListener = object: TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?,
                                                 width: Int,
                                                 height: Int) {
            configureTransform()
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return false
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?,
                                               width: Int,
                                               height: Int) {
            if (openCameraRequested) {
                open()
            }
        }
    }

    private val captureCallback = object: CameraCaptureSession.CaptureCallback() {

        private fun process(result: CaptureResult) {
            try {
                when(state) {
                    State.WAITING_LOCK -> {
                        val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                        if (afState == null || afState == 0) {
                            captureStillPicture()
                        }
                        else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                            // CONTROL_AE_STATE can be null on some devices
                            val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                            if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                                state = State.PICTURE_TAKEN
                                captureStillPicture()
                            } else {
                                runPrecaptureSequence()
                            }
                        }
                    }
                    State.WAITING_PRECAPTURE -> {
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED
                        ) {
                            state = State.WAITING_NON_PRECAPTURE
                        }
                    }
                    State.WAITING_NON_PRECAPTURE -> {
                        // CONTROL_AE_STATE can be null on some devices
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                            state = State.PICTURE_TAKEN
                            captureStillPicture()
                        }
                    }
                }
            }
            catch (e: Throwable) {
                close()
                onErrorCallback(e)
            }
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            process(result)
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            process(partialResult)
        }
    }

    private fun runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            previewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            state = State.WAITING_PRECAPTURE
            captureSession!!.capture(
                previewRequestBuilder!!.build(), captureCallback,
                backgroundHandler
            )
        } catch (e: Throwable) {
            close()
            onErrorCallback(e)
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        imageReader?.acquireLatestImage()?.apply {
            backgroundHandler.run {
                val buffer = planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                onPhotoTakenCallback(bytes)
            }
        }
    }

    private fun getOrientation(rotation: Int): Int {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        val degrees = when(rotation) {
            Surface.ROTATION_0 -> 90
            Surface.ROTATION_90 -> 0
            Surface.ROTATION_180 -> 270
            Surface.ROTATION_270 -> 180
            else -> throw IllegalStateException()
        }
        return (degrees + sensorOrientation + 270) % 360
    }

    fun captureStillPicture() {
        if (cameraDevice == null) {
            return
        }

        state = State.PICTURE_CAPTURING

        try {
            val captureBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader!!.surface)
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            val rotation = display.rotation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation))
            captureSession?.apply {
                stopRepeating()
                abortCaptures()
                capture(captureBuilder.build(), null, null)
            }
        }
        catch (e: Throwable) {
            close()
            onErrorCallback(e)
        }
    }

    override fun open() {
        try {
            if (!preview.isAvailable) {
                openCameraRequested = true
                return
            }
            startBackgroundThread()
            setUpCameraOutputs()
            configureTransform()

            cameraManager.openCamera(cameraId!!, stateCallback, backgroundHandler)
        }
        catch (e: Throwable) {
            close()
            onErrorCallback(e)
        }
    }

    private fun setUpCameraOutputs() {
        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)

            val facing = characteristics[CameraCharacteristics.LENS_FACING]
            if (facing != CameraCharacteristics.LENS_FACING_BACK) {
                continue
            }

            val streamConfigurationMap = characteristics[
                    SCALER_STREAM_CONFIGURATION_MAP] ?: continue

            photoSize = getMaxAvailableSize(
                streamConfigurationMap.getOutputSizes(ImageFormat.JPEG), maxWidth, maxHeight)

            imageReader = ImageReader.newInstance(photoSize.width, photoSize.height,
                ImageFormat.JPEG,1).apply {
                setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
            }

            val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
            var swappedDimensions = false

            when(display.rotation) {
                Surface.ROTATION_0,
                Surface.ROTATION_180 -> {
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        swappedDimensions = true
                    }
                }
                Surface.ROTATION_90,
                Surface.ROTATION_270 -> {
                    if (sensorOrientation == 0 || sensorOrientation == 180) {
                        swappedDimensions = true
                    }
                }
            }

            val displaySize = Point()
            display.getSize(displaySize)
            var rotatedPreviewWidth = preview.width
            var rotatedPreviewHeight = preview.height
            var maxPreviewWidth = displaySize.x
            var maxPreviewHeight = displaySize.y

            if (swappedDimensions) {
                rotatedPreviewWidth = preview.height
                rotatedPreviewHeight = preview.width
                maxPreviewWidth = displaySize.y
                maxPreviewHeight = displaySize.x
            }

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH
            }

            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = MAX_PREVIEW_HEIGHT
            }

            previewSize = chooseOptimalSize(streamConfigurationMap.getOutputSizes(SurfaceTexture::class.java),
                rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                maxPreviewHeight, photoSize)

            // todo разобраться, почему без setAspectRatio всё хорошо, но с ним - всё плохо
            /*val orientation = context.resources.configuration.orientation

            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                preview.setAspectRatio(previewSize.width, previewSize.height)
            }
            else {
                preview.setAspectRatio(previewSize.height, previewSize.width)
            }*/

            cameraId = id
        }
    }

    private fun configureTransform() {
        val viewWidth = preview.width
        val viewHeight = preview.height
        val rotation = display.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f,
            previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate(90f * (rotation - 2), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        preview.setTransform(matrix)
    }

    private fun chooseOptimalSize(
        choices: Array<Size>, textureViewWidth: Int,
        textureViewHeight: Int, maxWidth: Int, maxHeight: Int, aspectRatio: Size
    ): Size {

        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough = ArrayList<Size>()
        // Collect the supported resolutions that are smaller than the preview Surface
        val notBigEnough = ArrayList<Size>()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.width <= maxWidth && option.height <= maxHeight &&
                option.height == option.width * h / w
            ) {
                if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                    bigEnough.add(option)
                } else {
                    notBigEnough.add(option)
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size > 0) {
            return Collections.min(bigEnough, CompareSizesByArea())
        }
        else if (notBigEnough.size > 0) {
            return Collections.max(notBigEnough, CompareSizesByArea())
        }
        else {
            return choices[0]
        }
    }

    override fun setDisplay(display: Display) {
        this.display = display
    }

    private fun getMaxAvailableSize(sizes: Array<Size>,
                                    maxWidth: Int = -1,
                                    maxHeight: Int = -1): Size {
        sizes.sortByDescending {
            it.width * it.height
        }
        if (maxWidth == -1 || maxHeight == -1) {
            return sizes[0]
        }
        for (size in sizes) {
            if (size.width <= maxWidth && size.height <= maxHeight) {
                return size
            }
        }
        throw IllegalStateException()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera Background").apply {
            start()
            backgroundHandler = Handler(looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
        } catch (e: InterruptedException) {
            Timber.e(e)
        }
    }

    private fun createCameraPreviewSession() {
        val texture = preview.surfaceTexture!!

        texture.setDefaultBufferSize(previewSize.width, previewSize.height)

        val surface = Surface(texture)

        previewRequestBuilder = cameraDevice!!
            .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(surface)
        }

        cameraDevice!!.createCaptureSession(arrayListOf(surface, imageReader!!.surface),
            object: CameraCaptureSession.StateCallback() {
            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                Timber.i("onConfigured")
                if (cameraDevice == null) {
                    return
                }
                captureSession = cameraCaptureSession

                previewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

                previewRequest = previewRequestBuilder!!.build()
                cameraCaptureSession.setRepeatingRequest(
                    previewRequest!!, captureCallback, backgroundHandler)
            }

            override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                Timber.i("onConfigureFailed")
            }
        }, backgroundHandler)
    }

    override fun close() {
        try {
            stopBackgroundThread()
            preview.surfaceTextureListener = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            captureSession?.close()
        }
        catch (e: Throwable) {
            Timber.e(e)
        }
    }

    override fun takePhoto() {
        if (cameraDevice == null) {
            return
        }
        lockFocus()
    }

    private fun lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            previewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START
            )
            // Tell #mCaptureCallback to wait for the lock.
            state = State.WAITING_LOCK
            captureSession!!.capture(
                previewRequestBuilder!!.build(), captureCallback,
                backgroundHandler
            )
        } catch (e: Throwable) {
            close()
            onErrorCallback(e)
        }

    }

    override fun setMaxWidth(width: Int) {
        maxWidth = width
    }

    override fun setMaxHeight(height: Int) {
        maxHeight = height
    }

    override fun setPreview(preview: Any) {
        this.preview = preview as AutoFitTextureView

        preview.surfaceTextureListener = textureListener
    }

    override fun setOnErrorCallback(callback: (Throwable) -> Unit) {
        onErrorCallback = callback
    }

    override fun setOnPhotoTakenCallback(callback: (ByteArray) -> Unit) {
        onPhotoTakenCallback = callback
    }

    class CompareSizesByArea : Comparator<Size> {
        override fun compare(lhs: Size, rhs: Size): Int {
            return java.lang.Long.signum(
                lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
        }

    }
}