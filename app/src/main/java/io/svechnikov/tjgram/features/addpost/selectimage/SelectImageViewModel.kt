package io.svechnikov.tjgram.features.addpost.selectimage

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.base.SingleLiveEvent
import io.svechnikov.tjgram.features.addpost.selectimage.usecases.CheckAuth
import io.svechnikov.tjgram.features.addpost.selectimage.usecases.InvalidImageDimensionsException
import io.svechnikov.tjgram.features.addpost.selectimage.usecases.TooLargeImageSizeException
import io.svechnikov.tjgram.features.addpost.selectimage.usecases.ValidateLocalImage
import javax.inject.Inject

class SelectImageViewModel @Inject constructor(
    private val validateLocalImage: ValidateLocalImage,
    private val context: Context,
    private val checkAuth: CheckAuth
    ): ViewModel() {

    private val eventMutable = SingleLiveEvent<SelectImageEvent>()
    val event: LiveData<SelectImageEvent> = eventMutable

    private val stateMutable = MutableLiveData<SelectImageState>()
    val state: LiveData<SelectImageState> = stateMutable

    private var triedToAuthenticate = false
    private var isCheckingAuthentication = false
    private var authenticated = false
    private var firstLaunch = true

    /**
     * Eсли Observer подпишется после того, как появится значение, то SingleLiveEvent
     * не вызовет Observer.onChanged (такая ситуация актуальна в случае с дочерними фрагментами)
     * Поэтому используем MutableLiveData с nullable типом
     * todo придумать решение поэлегантнее
     */
    private val storagePermissionsMutable = MutableLiveData<Boolean?>()
    val storagePermissions: LiveData<Boolean?> = storagePermissionsMutable

    private val cameraPermissionsMutable = MutableLiveData<Boolean?>()
    val cameraPermissions: LiveData<Boolean?> = cameraPermissionsMutable

    fun onEvent(event: SelectImageEvent) {
        eventMutable.value = event
    }

    fun imageSelected(id: Int) {
        validateLocalImage(id) {
            it.either(::onError) {
                eventMutable.value = SelectImageEvent.OpenSendImage(id)
            }
        }
    }

    fun storagePermissionsHandled() {
        storagePermissionsMutable.value = null
    }

    fun cameraPermissionsHandled() {
        cameraPermissionsMutable.value = null
    }

    fun start() {
        isCheckingAuthentication = true
        checkAuth(Unit) {
            it.either({
                val message = getErrorMessage(it)
                eventMutable.postValue(SelectImageEvent.ShowError(message))
                isCheckingAuthentication = false
            }, {
                authenticated = it
                if (!authenticated) {
                    eventMutable.value = when(triedToAuthenticate) {
                        true -> {
                            SelectImageEvent.GoBack
                        }
                        false -> {
                            triedToAuthenticate = true
                            SelectImageEvent.NavigateToAuth
                        }
                    }
                }
                else {
                    if (firstLaunch) {
                        when(state.value) {
                            SelectImageState.ShowPickImageScreen -> {
                                eventMutable.value =
                                    SelectImageEvent.RequestGalleryPermissions
                            }
                            SelectImageState.ShowTakePhotoScreen -> {
                                eventMutable.value =
                                    SelectImageEvent.RequestCameraPermissions
                            }
                        }
                    }
                    firstLaunch = false
                }
                isCheckingAuthentication = false
            })
        }
    }

    fun onStoragePermissionsResult(result: Boolean) {
        storagePermissionsMutable.value = result
    }

    fun onCameraPermissionsResult(result: Boolean) {
        cameraPermissionsMutable.value = result
    }

    fun onPickImageSelected() {
        if (stateMutable.value != SelectImageState.ShowPickImageScreen) {
            stateMutable.value = SelectImageState.ShowPickImageScreen
            if (authenticated) {
                eventMutable.value =
                    SelectImageEvent.RequestGalleryPermissions
            }
        }
    }

    fun onCameraSelected() {
        if (stateMutable.value != SelectImageState.ShowTakePhotoScreen) {
            stateMutable.value = SelectImageState.ShowTakePhotoScreen
            if (authenticated) {
                eventMutable.value =
                    SelectImageEvent.RequestCameraPermissions
            }
        }
    }

    private fun getErrorMessage(error: Throwable): String {
        return when(error) {
            is TooLargeImageSizeException -> {
                val size = error.size.toFloat() / 1024 / 1024
                val maxSize = error.maxSize.toFloat() / 1024 / 1024
                context.getString(R.string.pick_image_too_large_error, size, maxSize)
            }
            is InvalidImageDimensionsException -> {
                context.getString(R.string.pick_image_invalid_dimensions, error.dimensions)
            }
            else -> {
                context.getString(R.string.generic_error)
            }
        }
    }

    private fun onError(e: Throwable) {
        eventMutable.postValue(
            SelectImageEvent.ShowError(
                getErrorMessage(e)
            )
        )
    }
}