package io.svechnikov.tjgram.features.addpost.sendimage.usecases

import io.svechnikov.tjgram.base.LocalImages
import io.svechnikov.tjgram.base.UseCase
import io.svechnikov.tjgram.base.data.LocalImage
import javax.inject.Inject

class FetchImage @Inject constructor(
    private val localImages: LocalImages

): UseCase<Int, LocalImage>() {
    override suspend fun run(params: Int): LocalImage {
        return localImages.getImageById(params)!!
    }
}