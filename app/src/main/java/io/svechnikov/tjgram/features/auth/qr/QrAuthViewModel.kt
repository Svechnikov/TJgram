package io.svechnikov.tjgram.features.auth.qr

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.base.exceptions.NoConnectionException
import io.svechnikov.tjgram.base.exceptions.ServiceException
import javax.inject.Inject

class QrAuthViewModel @Inject constructor(
    private val authWithQrToken: AuthWithQrToken,
    private val context: Context
): ViewModel() {

    private val eventMutable = MutableLiveData<QrAuthEvent?>()
    val event: LiveData<QrAuthEvent?> = eventMutable

    private val stateMutable = MutableLiveData<QrAuthState>()
    val state: LiveData<QrAuthState> = stateMutable

    init {
        stateMutable.value = QrAuthState.Empty
        eventMutable.value = QrAuthEvent.RequestPermissions
    }

    override fun onCleared() {
        super.onCleared()

        authWithQrToken.cancel()
    }

    fun onGotPermissions() {
        stateMutable.value = QrAuthState.ShowScanner
    }

    fun eventHandled() {
        eventMutable.value = null
    }

    fun onQrResult(token: String) {
        authWithQrToken(AuthWithQrToken.Params(token)) {
            it.either(::onError) {
                eventMutable.value = QrAuthEvent.Success
            }
        }
    }

    private fun onError(throwable: Throwable) {
        val errorMessage = when(throwable) {
            is NoConnectionException -> {
                context.getString(R.string.no_connection_error)
            }
            is ServiceException -> {
                throwable.serviceMessage
            }
            else -> {
                context.getString(R.string.generic_error)
            }
        }

        eventMutable.value = QrAuthEvent.CloseWithError(errorMessage)
    }
}

sealed class QrAuthEvent {
    data class CloseWithError(val message: String) : QrAuthEvent()
    object Success : QrAuthEvent()
    object RequestPermissions : QrAuthEvent()
}

enum class QrAuthState {
    Empty,
    ShowScanner
}