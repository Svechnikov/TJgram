package io.svechnikov.tjgram.features.addpost.selectimage.usecases

import io.svechnikov.tjgram.base.LocalImages
import io.svechnikov.tjgram.base.UseCase
import javax.inject.Inject

class ValidateLocalImage @Inject constructor(
    private val localImages: LocalImages
): UseCase<Int, Unit>() {

    companion object {
        private const val MAX_SIZE = 20 * 1024 * 1024
    }

    override suspend fun run(params: Int) {
        val image = localImages.getImageById(params)!!


        if (image.size > MAX_SIZE) {
            throw TooLargeImageSizeException(
                image.size,
                MAX_SIZE
            )
        }

        // по непонятным причинам ContentResolver может отдавать размеры 0х0
        // todo разобраться
        /*if (image.width !in 200..10000) {
            throw InvalidImageDimensionsException("${image.width}x${image.height}")
        }

        if (image.height !in 200..10000) {
            throw InvalidImageDimensionsException("${image.width}x${image.height}")
        }*/
    }
}

class TooLargeImageSizeException(val size: Int,
                                 val maxSize: Int) : Exception()

class InvalidImageDimensionsException(val dimensions: String) : Exception()