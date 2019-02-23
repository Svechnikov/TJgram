package io.svechnikov.tjgram.features.addpost.selectimage.pickimage

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import androidx.paging.toLiveData
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.base.SingleLiveEvent
import io.svechnikov.tjgram.features.addpost.selectimage.pickimage.usecases.FetchLocalImages
import timber.log.Timber
import javax.inject.Inject

class PickImageViewModel @Inject constructor(
    fetchImages: FetchLocalImages,
    private val context: Context
    ): ViewModel() {

    private val eventMutable = SingleLiveEvent<PickImageEvent>()
    val event: LiveData<PickImageEvent> = eventMutable

    private val stateMutable = MutableLiveData<PickImageState>()
    val state: LiveData<PickImageState> = stateMutable

    private var gotPermissions: Boolean? = null

    private val dataSourceFactory = ImageDataSource.Factory(
        fetchImages = fetchImages,
        stateCallback = {
            stateMutable.postValue(it)
        },
        errorCallback = {
            eventMutable.value = PickImageEvent.ShowError(
                context.getString(R.string.generic_error))
        }
    )

    private val imagesSource = dataSourceFactory.map {
        val thumbPath = when(it.thumbPath) {
            null -> null
            else -> "file:${it.thumbPath}"
        }
        PickImageView(
            id = it.id,
            path = "file:${it.path}",
            thumbPath = thumbPath
        )
    }.toLiveData(pageSize = 80)

    val images: LiveData<PagedList<PickImageView>> =
        object: MediatorLiveData<PagedList<PickImageView>>() {
            init {
                addSource(state) {
                    if (it == PickImageState.Loading) {
                        removeSource(state)
                        addSource(imagesSource) {list ->
                            value = list
                        }
                    }
                }
            }
    }

    fun onPermissionsResult(result: Boolean) {
        if (result != gotPermissions) {
            gotPermissions = result
            if (result) {
                stateMutable.value = PickImageState.Loading
            }
            else {
                stateMutable.value = PickImageState.PermissionsRejected
            }
        }
    }

    fun refreshFileImages() {
        dataSourceFactory.dataSource.value?.invalidate()
    }
}

sealed class PickImageEvent {
    data class ShowError(val message: String) : PickImageEvent()
}

