package io.svechnikov.tjgram.base.data

data class UploadedImage(
    val type: String,
    val data: Data
) {
    data class Data(
        val uuid: String,
        val width: Int,
        val height: Int,
        val size: Int,
        val type: String,
        val color: String
    )
}