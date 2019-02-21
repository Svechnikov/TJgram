package io.svechnikov.tjgram.features.addpost.selectimage

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import androidx.paging.toLiveData
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.base.SingleLiveEvent
import io.svechnikov.tjgram.features.addpost.selectimage.usecases.*
import javax.inject.Inject

class SelectImageViewModel @Inject constructor(
    fetchImages: FetchLocalImages,
    private val context: Context,
    private val validateLocalImage: ValidateLocalImage,
    private val checkAuth: CheckAuth
    ): ViewModel() {

    private val eventMutable = SingleLiveEvent<SelectImageEvent>()
    val event: LiveData<SelectImageEvent> = eventMutable

    private val stateMutable = MutableLiveData<SelectImageState>()
    val state: LiveData<SelectImageState> = stateMutable

    private val dataSourceFactory = ImageDataSource.Factory(
        fetchImages = fetchImages,
        stateCallback = {
            stateMutable.postValue(it)
        },
        errorCallback = ::onError
    )

    private val imagesSource = dataSourceFactory.map {
        val thumbPath = when(it.thumbPath) {
            null -> null
            else -> "file:${it.thumbPath}"
        }
        SelectImageView(
            id = it.id,
            path = "file:${it.path}",
            thumbPath = thumbPath
        )
    }.toLiveData(pageSize = 80)

    val images: LiveData<PagedList<SelectImageView>> =
        object: MediatorLiveData<PagedList<SelectImageView>>() {
            init {
                addSource(state) {
                    if (it == SelectImageState.Loading) {
                        removeSource(state)
                        addSource(imagesSource) {list ->
                            value = list
                        }
                    }
                }
            }
    }

    private var triedToAuthenticate = false

    fun start() {
        checkAuth(Unit) {
            it.either({
                eventMutable.postValue(
                    SelectImageEvent.ShowError(
                        context.getString(R.string.generic_error))
                )
            }, {authenticated ->
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
                    eventMutable.value =
                        SelectImageEvent.RequestPermissions
                }
            })
        }
    }


    fun onGotPermissions() {
        stateMutable.value = SelectImageState.Loading
    }

    fun imageSelected(SelectImageView: SelectImageView) {
        validateLocalImage(SelectImageView.id) {
            it.either(::onError) {
                eventMutable.value = SelectImageEvent.OpenSendImage(SelectImageView.id)
            }
        }
    }

    fun refreshFileImages() {
        dataSourceFactory.dataSource.value?.invalidate()
    }

    private fun getErrorMessage(error: Throwable): String {
        return when(error) {
            is TooLargeImageSizeException -> {
                val size = error.size.toFloat() / 1024 / 1024
                val maxSize = error.maxSize.toFloat() / 1024 / 1024
                context.getString(R.string.select_image_too_large_error, size, maxSize)
            }
            is InvalidImageDimensionsException -> {
                context.getString(R.string.select_image_invalid_dimensions, error.dimensions)
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

sealed class SelectImageEvent {
    data class ShowError(val message: String) : SelectImageEvent()
    data class OpenSendImage(val imageId: Int) : SelectImageEvent()
    object RequestPermissions : SelectImageEvent()
    object GoBack : SelectImageEvent()
    object NavigateToAuth : SelectImageEvent()
}

