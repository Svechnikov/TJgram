package io.svechnikov.tjgram.features.addpost.selectimage.pickimage.usecases

import io.svechnikov.tjgram.base.LocalImages
import io.svechnikov.tjgram.base.UseCase
import io.svechnikov.tjgram.base.data.LocalImage
import io.svechnikov.tjgram.base.db.BaseDatabase
import io.svechnikov.tjgram.base.exceptions.NotAuthenticatedException
import javax.inject.Inject

class FetchLocalImages @Inject constructor(
    private val db: BaseDatabase,
    private val localImages: LocalImages
): UseCase<FetchLocalImages.Params, List<LocalImage>>() {

    override suspend fun run(params: Params): List<LocalImage> {
        db.user().user() ?: throw NotAuthenticatedException()

        val positionFrom = params.positionFrom
        val loadSize = params.loadSize

        return localImages.getLocalImages(positionFrom, loadSize)
    }

    data class Params(val positionFrom: Int, val loadSize: Int)
}