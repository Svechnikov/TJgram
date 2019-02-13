package io.svechnikov.tjgram.features.addpost.sendimage.usecases

import io.svechnikov.tjgram.base.UseCase
import io.svechnikov.tjgram.base.data.LocalImage
import io.svechnikov.tjgram.base.db.BaseDatabase
import io.svechnikov.tjgram.base.network.Gateway
import javax.inject.Inject


class SendImage @Inject constructor(
    private val gateway: Gateway,
    private val db: BaseDatabase
): UseCase<SendImage.Params, Unit>() {

    override suspend fun run(params: Params) {
        val uploadedImage = gateway.uploadImage(params.image.path)

        gateway.createPost(params.title, params.text, uploadedImage)

        db.posts().clearAll()
    }

    data class Params(val title: String,
                      val text: String,
                      val image: LocalImage)
}