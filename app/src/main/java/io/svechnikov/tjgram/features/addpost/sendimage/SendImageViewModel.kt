package io.svechnikov.tjgram.features.addpost.sendimage

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.base.SingleLiveEvent
import io.svechnikov.tjgram.base.data.LocalImage
import io.svechnikov.tjgram.features.addpost.sendimage.usecases.FetchImage
import io.svechnikov.tjgram.features.addpost.sendimage.usecases.SendImage
import timber.log.Timber
import javax.inject.Inject

class SendImageViewModel @Inject constructor(
    private val fetchImage: FetchImage,
    private val sendImage: SendImage,
    private val context: Context
): ViewModel() {

    private val localImageMutable = MutableLiveData<LocalImage>()
    val image: LiveData<SendImageView> = Transformations.map(localImageMutable) {
        val path = when(it.thumbPath) {
            null -> {
                "file:${it.path}"
            }
            else -> {
                "file:${it.thumbPath}"
            }
        }
        SendImageView(path)
    }

    private val eventMutable = SingleLiveEvent<SendImageEvent>()
    val event: LiveData<SendImageEvent> = eventMutable

    private val stateMutable = MutableLiveData<SendImageState>()
    val state: LiveData<SendImageState> = stateMutable

    private var id = 0

    fun setImageId(id: Int) {
        if (this.id != id) {
            this.id = id
            fetchImage(id) {
                it.either({
                    it.printStackTrace()
                }, {
                    localImageMutable.value = it
                })
            }
        }
    }

    fun send(title: String, text: String) {
        stateMutable.value = SendImageState.UPLOADING
        localImageMutable.value?.let {
            sendImage(SendImage.Params(title, text, it)) {
                it.either(::handleError) {
                    stateMutable.value = SendImageState.IDLE
                    eventMutable.value = SendImageEvent.Success(
                        context.getString(R.string.send_image_success))
                }
            }
        }
    }

    private fun handleError(e: Throwable) {
        Timber.e(e)
        stateMutable.value = SendImageState.IDLE
        val message = context.getString(R.string.generic_error)
        eventMutable.value = SendImageEvent.ShowError(message)
    }
}

enum class SendImageState {
    IDLE,
    UPLOADING
}

sealed class SendImageEvent {
    data class ShowError(val message: String) : SendImageEvent()
    data class Success(val message: String) : SendImageEvent()
}