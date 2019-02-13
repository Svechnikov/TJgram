package io.svechnikov.tjgram.base

import android.content.Context
import android.provider.MediaStore
import io.svechnikov.tjgram.base.data.LocalImage
import javax.inject.Inject

class LocalImages @Inject constructor(
    private val context: Context
) {

    fun getImageById(id: Int): LocalImage? {
        val images = getImagesBySelection(positionFrom = 0,
            loadSize = 1,
            selection = "${MediaStore.Images.ImageColumns._ID} = ?",
            selectionArgs = arrayOf(id.toString()))

        if (images.isEmpty()) {
            return null
        }
        return images[0]
    }

    fun getLocalImages(positionFrom: Int, loadSize: Int): List<LocalImage> {
        return getImagesBySelection(positionFrom, loadSize)
    }

    private fun getImagesBySelection(positionFrom: Int,
                                     loadSize: Int,
                                     selection: String? = null,
                                     selectionArgs: Array<String>? = null): List<LocalImage> {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.MINI_THUMB_MAGIC,
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.WIDTH,
            MediaStore.Images.ImageColumns.HEIGHT,
            MediaStore.Images.ImageColumns.SIZE)

        val cursor = context.contentResolver.query(uri,
            projection, selection, selectionArgs,
            "${MediaStore.Images.ImageColumns.DATE_ADDED} desc")!!

        cursor.apply {
            val idIndex = getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID)
            val thumbIndex = getColumnIndexOrThrow(MediaStore.Images.ImageColumns.MINI_THUMB_MAGIC)
            val dataIndex = getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA)
            val widthIndex = getColumnIndexOrThrow(MediaStore.Images.ImageColumns.WIDTH)
            val heightIndex = getColumnIndexOrThrow(MediaStore.Images.ImageColumns.HEIGHT)
            val sizeIndex = getColumnIndexOrThrow(MediaStore.Images.ImageColumns.SIZE)

            val contentProviderImages = arrayListOf<ContentProviderImage>()
            for (position in positionFrom..positionFrom + loadSize) {
                if (moveToPosition(position)) {
                    contentProviderImages.add(
                        ContentProviderImage(
                            id = getInt(idIndex),
                            thumbId = getInt(thumbIndex),
                            path = getString(dataIndex),
                            width = getInt(widthIndex),
                            height = getInt(heightIndex),
                            size = getInt(sizeIndex)
                        )
                    )
                }
                else {
                    break
                }
            }

            close()

            /**
             * For some images there may not be thumbnails
             */
            val thumbnails = getThumbnails(contentProviderImages.joinToString {
                it.id.toString()
            })

            return contentProviderImages.map {
                val thumb = thumbnails[it.id]

                val thumbPath = when(thumb) {
                    null -> {
                        null
                    }
                    else -> {
                        thumb
                    }
                }

                LocalImage(
                    id = it.id,
                    path = it.path,
                    width = it.width,
                    height = it.height,
                    thumbPath = thumbPath,
                    size = it.size
                )
            }
        }
    }

    private fun getThumbnails(ids: String): HashMap<Int, String> {
        val thumbnails = HashMap<Int, String>()

        val selection = "${MediaStore.Images.Thumbnails.IMAGE_ID} IN ($ids)"
        val cursor = context.contentResolver.query(
            MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Thumbnails.DATA, MediaStore.Images.Thumbnails.IMAGE_ID),
            selection,
            null,
            null,
            null)!!

        cursor.apply {
            val dataIndex = getColumnIndexOrThrow(MediaStore.Images.Thumbnails.DATA)
            val idIndex = getColumnIndexOrThrow(MediaStore.Images.Thumbnails.IMAGE_ID)

            while (moveToNext()) {
                thumbnails[getInt(idIndex)] = getString(dataIndex)
            }

            close()
        }

        return thumbnails
    }

    data class ContentProviderImage(val id: Int,
                                    val thumbId: Int,
                                    val path: String,
                                    val width: Int,
                                    val height: Int,
                                    val size: Int)
}