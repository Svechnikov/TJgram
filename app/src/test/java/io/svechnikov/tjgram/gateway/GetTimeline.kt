package io.svechnikov.tjgram.gateway

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.svechnikov.tjgram.base.db.entities.Post
import io.svechnikov.tjgram.base.exceptions.NoConnectionException
import io.svechnikov.tjgram.base.exceptions.ServiceException
import io.svechnikov.tjgram.base.network.Gateway
import io.svechnikov.tjgram.base.network.HttpService
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import retrofit2.Call
import retrofit2.Response
import java.io.IOException

class GetTimeline {

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

        val gateway = Gateway(context, httpService, gson, SITE_ID)

        whenever(networkInfo.isConnected)
            .thenReturn(false)

        whenever(connectivityManager.activeNetworkInfo)
            .thenReturn(networkInfo)

        whenever(context.getSystemService(eq(Context.CONNECTIVITY_SERVICE)))
            .thenReturn(connectivityManager)

        try {
            gateway.getTimeline(Post.Sorting.TOP_MONTH, 10, 0)
            fail()
        }
        catch (e: NoConnectionException) {

        }

        verifyZeroInteractions(httpService)
    }

    @Test @Suppress("UNCHECKED_CAST")
    fun jsonMalformedError() {
        MockitoAnnotations.initMocks(this)

        val gateway = Gateway(context, httpService, gson, SITE_ID)

        whenever(networkInfo.isConnected)
            .thenReturn(true)

        whenever(connectivityManager.activeNetworkInfo)
            .thenReturn(networkInfo)

        whenever(context.getSystemService(eq(Context.CONNECTIVITY_SERVICE)))
            .thenReturn(connectivityManager)

        val errorBodyText = "error body text"
        val call = Mockito.mock(Call::class.java) as Call<List<Post>>
        val response = Mockito.mock(Response::class.java) as Response<List<Post>>
        val errorBody = Mockito.mock(ResponseBody::class.java)

        whenever(httpService.getSubsiteTimeline(eq(SITE_ID),
            eq("top/month"), eq(10), eq(0))).thenReturn(call)
        whenever(call.execute())
            .thenReturn(response)
        whenever(response.isSuccessful)
            .thenReturn(false)
        whenever(response.errorBody())
            .thenReturn(errorBody)
        whenever(errorBody.string())
            .thenReturn(errorBodyText)
        whenever(gson.fromJson(eq(errorBodyText), eq(ServiceException::class.java)))
            .thenThrow(JsonSyntaxException("Invalid json"))

        try {
            gateway.getTimeline(Post.Sorting.TOP_MONTH, 10, 0)
            fail("Exception wasn't thrown")
        }
        catch (e: IOException) {
        }
    }

    @Test @Suppress("UNCHECKED_CAST")
    fun serviceError() {
        MockitoAnnotations.initMocks(this)

        val gateway = Gateway(context, httpService, gson, SITE_ID)

        whenever(networkInfo.isConnected)
            .thenReturn(true)

        whenever(connectivityManager.activeNetworkInfo)
            .thenReturn(networkInfo)

        whenever(context.getSystemService(eq(Context.CONNECTIVITY_SERVICE)))
            .thenReturn(connectivityManager)

        val errorBodyText = "error body text"
        val call = Mockito.mock(Call::class.java) as Call<List<Post>>
        val response = Mockito.mock(Response::class.java) as Response<List<Post>>
        val errorBody = Mockito.mock(ResponseBody::class.java)
        val expectedException = ServiceException(400, "Some error")

        whenever(httpService.getSubsiteTimeline(eq(SITE_ID),
            eq("top/month"), eq(10), eq(0))).thenReturn(call)
        whenever(call.execute())
            .thenReturn(response)
        whenever(response.isSuccessful)
            .thenReturn(false)
        whenever(response.errorBody())
            .thenReturn(errorBody)
        whenever(errorBody.string())
            .thenReturn(errorBodyText)
        whenever(gson.fromJson(eq(errorBodyText), eq(ServiceException::class.java)))
            .thenReturn(expectedException)

        try {
            gateway.getTimeline(Post.Sorting.TOP_MONTH, 10, 0)
            fail("Exception wasn't thrown")
        }
        catch (e: ServiceException) {
            assertEquals(expectedException, e)
        }
    }
}