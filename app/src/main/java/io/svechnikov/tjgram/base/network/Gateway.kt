package io.svechnikov.tjgram.base.network

import android.content.Context
import android.net.ConnectivityManager
import com.google.gson.Gson
import io.svechnikov.tjgram.base.data.CreatePostResult
import io.svechnikov.tjgram.base.data.LikePostResult
import io.svechnikov.tjgram.base.data.UploadedImage
import io.svechnikov.tjgram.base.db.entities.Post
import io.svechnikov.tjgram.base.db.entities.User
import io.svechnikov.tjgram.base.exceptions.NoConnectionException
import io.svechnikov.tjgram.base.exceptions.ServiceException
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named


class Gateway @Inject constructor(
    private val context: Context,
    private val service: HttpService,
    private val gson: Gson,
    @Named("site_id") private val siteId: Long
) {
    fun getTimeline(sorting: Post.Sorting,
                    count: Int,
                    offset: Int): List<Post> {
        if (!isNetworkAvailable()) {
            throw NoConnectionException()
        }

        val sortingValue = when(sorting) {
            Post.Sorting.NEW -> "new"
            Post.Sorting.TOP_WEEK -> "top/week"
            Post.Sorting.TOP_MONTH -> "top/month"
            Post.Sorting.TOP_YEAR -> "top/year"
            Post.Sorting.TOP_ALL -> "top/all"
            else -> throw IllegalArgumentException()
        }

        val response = service.getSubsiteTimeline(
            siteId, sortingValue, count, offset).execute()

        checkError(response)
        return response.body() ?: throw IOException()
    }

    fun authQr(token: String): User {
        if (!isNetworkAvailable()) {
            throw NoConnectionException()
        }

        val mediaType = MediaType.parse("text/plain")
        val requestBody = RequestBody.create(mediaType, token)

        val response = service.authQr(requestBody).execute()
        checkError(response)

        val deviceToken = response.headers().get("x-device-token") ?: throw IOException()
        val user = response.body() ?: throw IOException()

        return user.copy(deviceToken = deviceToken)
    }

    fun createPost(title: String,
                   text: String,
                   uploadedImage: UploadedImage
    ): CreatePostResult {
        if (!isNetworkAvailable()) {
            throw NoConnectionException()
        }
        val plainMediaType = MediaType.parse("text/plain")

        val titleBody = RequestBody.create(plainMediaType, title)
        val textBody = RequestBody.create(plainMediaType, text)
        val subsiteIdBody = RequestBody.create(plainMediaType, siteId.toString())

        val attachmentBody = ArrayList<MultipartBody.Part>()

        // todo найти способ сериализовать данные в запрос автоматически
        // вообще, не совсем понятно, зачем слать объекты attaches целиком,
        // ведь можно было бы обойтись одним идентификатором
        attachmentBody.add(MultipartBody.Part.createFormData("attaches[0][type]",
            null, RequestBody.create(plainMediaType, uploadedImage.type)))

        attachmentBody.add(MultipartBody.Part.createFormData("attaches[0][data][type]",
            null, RequestBody.create(plainMediaType, uploadedImage.type)))

        attachmentBody.add(MultipartBody.Part.createFormData("attaches[0][data][data][uuid]",
            null, RequestBody.create(plainMediaType, uploadedImage.data.uuid)))

        attachmentBody.add(MultipartBody.Part.createFormData("attaches[0][data][data][width]",
            null, RequestBody.create(plainMediaType, uploadedImage.data.width.toString())))

        attachmentBody.add(MultipartBody.Part.createFormData("attaches[0][data][data][height]",
            null, RequestBody.create(plainMediaType, uploadedImage.data.height.toString())))

        attachmentBody.add(MultipartBody.Part.createFormData("attaches[0][data][data][size]",
            null, RequestBody.create(plainMediaType, uploadedImage.data.size.toString())))

        attachmentBody.add(MultipartBody.Part.createFormData("attaches[0][data][data][type]",
            null, RequestBody.create(plainMediaType, uploadedImage.data.type)))

        attachmentBody.add(MultipartBody.Part.createFormData("attaches[0][data][data][color]",
            null, RequestBody.create(plainMediaType, uploadedImage.data.color)))


        val response = service.createPost(titleBody, textBody,
            subsiteIdBody, attachmentBody.toTypedArray()).execute()
        checkError(response)

        return response.body() ?: throw IOException()
    }

    fun uploadImage(imagePath: String): UploadedImage {
        if (!isNetworkAvailable()) {
            throw NoConnectionException()
        }

        val file = File(imagePath)

        val requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val formData = MultipartBody.Part.createFormData("file", file.name, requestBody)

        val response = service.uploadImage(formData).execute()
        checkError(response)

        return response.body() ?: throw IOException()
    }

    fun likePost(id: Long, like: Int): LikePostResult {
        if (!isNetworkAvailable()) {
            throw NoConnectionException()
        }
        val response = service.likePost(id, like).execute()

        checkError(response)

        return response.body() ?: throw IOException()
    }

    private fun checkError(response: Response<*>) {
        if (!response.isSuccessful) {
            try {
                val error = gson.fromJson(response.errorBody()?.string(),
                    ServiceException::class.java)

                throw error
            }
            catch (e: Throwable) {
                if (e is ServiceException) {
                    throw e
                }
                throw IOException()
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val service = context.getSystemService(Context.CONNECTIVITY_SERVICE)

        return (service as ConnectivityManager).activeNetworkInfo?.isConnected ?: false
    }
}