package io.svechnikov.tjgram.base.network

import io.svechnikov.tjgram.base.data.CreatePostResult
import io.svechnikov.tjgram.base.data.LikePostResult
import io.svechnikov.tjgram.base.data.UploadedImage
import io.svechnikov.tjgram.base.db.entities.Post
import io.svechnikov.tjgram.base.db.entities.User
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface HttpService {
    @GET("/v1.4/subsite/{subsiteId}/timeline/{sorting}")
    fun getSubsiteTimeline(@Path("subsiteId") subsiteId: Long,
                           @Path("sorting", encoded = true) sorting: String,
                           @Query("count") count: Int,
                           @Query("offset") offset: Int): Call<List<Post>>

    @Multipart @POST("/v1.4/auth/qr")
    fun authQr(@Part("token") token: RequestBody): Call<User>

    @Multipart @POST("/v1.4/uploader/upload")
    fun uploadImage(@Part file: MultipartBody.Part): Call<UploadedImage>

    @Multipart @POST("/v1.4/entry/create")
    fun createPost(@Part("title") title: RequestBody,
                   @Part("text") text: RequestBody,
                   @Part("subsite_id") subsiteId: RequestBody,
                   @Part attachment: Array<MultipartBody.Part>): Call<CreatePostResult>

    @POST("/v1.4/entry/{id}/likes") @FormUrlEncoded
    fun likePost(@Path("id") id: Long,
                 @Field("sign") sign: Int): Call<LikePostResult>
}