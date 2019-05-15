package io.svechnikov.tjgram.gateway

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.svechnikov.tjgram.base.data.UploadedImage
import io.svechnikov.tjgram.base.db.entities.Post
import io.svechnikov.tjgram.base.exceptions.NoConnectionException
import io.svechnikov.tjgram.base.network.Gateway
import io.svechnikov.tjgram.base.network.HttpService
import org.junit.Assert
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class GatewayUnitTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var httpService: HttpService

    @Mock
    lateinit var connectivityManager: ConnectivityManager

    @Mock
    lateinit var networkInfo: NetworkInfo

    @Mock
    lateinit var gson: Gson

    companion object {
        const val SITE_ID = 1234L
    }

    @Test
    fun noConnection() {
        MockitoAnnotations.initMocks(this)

        val gateway = Gateway(context, httpService, gson, GetTimeline.SITE_ID)

        whenever(networkInfo.isConnected)
            .thenReturn(false)

        whenever(connectivityManager.activeNetworkInfo)
            .thenReturn(networkInfo)

        whenever(context.getSystemService(eq(Context.CONNECTIVITY_SERVICE)))
            .thenReturn(connectivityManager)

        val methods = listOf({
            gateway.getTimeline(Post.Sorting.TOP_MONTH, 10, 0)
        }, {
            gateway.authQr("test-token")
        }, {
            val image = Mockito.mock(UploadedImage::class.java)
            gateway.createPost("test-title", "test-text", image)
        }, {
            gateway.uploadImage("test-path")
        }, {
            gateway.likePost(1234L, 1)
        })

        for (method in methods) {
            try {
                method.invoke()
                Assert.fail()
            }
            catch (e: NoConnectionException) {

            }
        }

        verifyZeroInteractions(httpService)
    }
}