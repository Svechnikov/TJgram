package io.svechnikov.tjgram.features.addpost.selectimage.takephoto

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.base.SingleLiveEvent
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TakePhotoViewModel @Inject constructor(
    private val saveImageUseCase: SaveImage,
    private val context: Context
): ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    val maxPhotoWidth = 3000
    val maxPhotoHeight = 3000

    private val stateMutable = MutableLiveData<TakePhotoState>()
    val state: LiveData<TakePhotoState> = stateMutable

    private val eventMutable = SingleLiveEvent<TakePhotoEvent>()
    val event: LiveData<TakePhotoEvent> = eventMutable

    var gotPermissions: Boolean? = null

    private var job: Job? = null

    override fun onCleared() {
        super.onCleared()

        saveImageUseCase.cancel()
        job?.cancel()
    }

    fun onPermissionsResult(result: Boolean) {
        if (result != gotPermissions) {
            gotPermissions = result
            if (!result) {
                stateMutable.value = TakePhotoState.PermissionsRejected
            }
            else {
                job?.cancel()
                job = launch {
                    // todo найти способ получше избегать тормозов интерфейса во время
                    // запуска камеры и переключения TabLayout
                    delay(500)
                    stateMutable.postValue(TakePhotoState.ShowViewfinder)
                }
            }
        }
    }

    fun onActive() {
        Timber.i("onActive")
        if (gotPermissions == true && state.value != TakePhotoState.ProcessingPhoto) {
            stateMutable.value = TakePhotoState.ShowViewfinder
        }
    }

    fun onInactive() {
        Timber.i("onInactive")
        saveImageUseCase.cancel()
        job?.cancel()
    }

    fun onCameraError() {
        stateMutable.value = TakePhotoState.ShowCameraError
    }

    fun onTakePhoto() {
        stateMutable.value = TakePhotoState.ProcessingPhoto
    }

    fun saveImage(data: ByteArray) {
        saveImageUseCase(data) {
            it.either({
                Timber.e(it)
                eventMutable.value = TakePhotoEvent.Error(getErrorMessage(it))
                stateMutable.value = TakePhotoState.ShowViewfinder
            }, {
                eventMutable.value = TakePhotoEvent.NavigateToSendImage(it)
                stateMutable.value = TakePhotoState.ShowViewfinder
            })
        }
    }

    private fun getErrorMessage(e: Throwable): String {
        return context.getString(R.string.generic_error)
    }
}