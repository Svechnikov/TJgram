package io.svechnikov.tjgram.features.addpost.selectimage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.PageKeyedDataSource
import io.svechnikov.tjgram.base.data.LocalImage
import io.svechnikov.tjgram.features.addpost.selectimage.usecases.FetchLocalImages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class ImageDataSource constructor(
    private val fetchImages: FetchLocalImages,
    private val stateCallback: (SelectImageState) -> Unit,
    private val errorCallback: (Throwable) -> Unit
): PageKeyedDataSource<Int, LocalImage>(), CoroutineScope {

    private var job: Job? = null
    private var firstLoading = true

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    override fun loadInitial(params: LoadInitialParams<Int>,
                             callback: LoadInitialCallback<Int, LocalImage>) {
        if (firstLoading) {
            stateCallback(SelectImageState.Loading)
        }
        else {
            stateCallback(SelectImageState.Refreshing)
        }

        fetchImages(FetchLocalImages.Params(0, params.requestedLoadSize)) {
            it.either({
                errorCallback(it)
                stateCallback(SelectImageState.Loaded)
            }, {
                stateCallback(SelectImageState.Loaded)
                val nextKey = if (it.size < params.requestedLoadSize) {
                    null
                }
                else {
                    it.size
                }
                callback.onResult(it, null, nextKey)
            })
        }
    }

    override fun loadAfter(params: LoadParams<Int>,
                           callback: LoadCallback<Int, LocalImage>) {
        fetchImages(FetchLocalImages.Params(params.key, params.requestedLoadSize)) {
            it.either({
                errorCallback(it)
            }, {
                val nextKey = if (it.size < params.requestedLoadSize) {
                    null
                }
                else {
                    params.key + it.size
                }
                callback.onResult(it, nextKey)
            })
        }
    }

    override fun loadBefore(params: LoadParams<Int>,
                            callback: LoadCallback<Int, LocalImage>) {

    }

    override fun invalidate() {
        super.invalidate()

        job?.cancel()
    }

    class Factory constructor(
        private val fetchImages: FetchLocalImages,
        private val stateCallback: (SelectImageState) -> Unit,
        private val errorCallback: (Throwable) -> Unit
    ): DataSource.Factory<Int, LocalImage>() {

        private val dataSourceMutable = MutableLiveData<ImageDataSource>()

        val dataSource: LiveData<ImageDataSource> = dataSourceMutable

        override fun create(): DataSource<Int, LocalImage> {
            val source = ImageDataSource(
                fetchImages,
                stateCallback,
                errorCallback
            )
            dataSourceMutable.postValue(source)
            return source
        }
    }
}