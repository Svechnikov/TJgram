package io.svechnikov.tjgram.features.addpost.selectimage.takephoto

import io.svechnikov.tjgram.base.LocalImages
import io.svechnikov.tjgram.base.UseCase
import javax.inject.Inject

class SaveImage @Inject constructor(
    private val localImages: LocalImages
): UseCase<ByteArray, Int>() {
    override suspend fun run(params: ByteArray): Int {
        return localImages.saveImage(params)
    }
}