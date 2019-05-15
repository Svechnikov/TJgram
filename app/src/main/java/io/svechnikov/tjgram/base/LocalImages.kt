package io.svechnikov.tjgram.base

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.base.data.LocalImage
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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

    fun saveImage(data: ByteArray): Int {
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val name = "${System.currentTimeMillis()}.jpg"
        val file = File(path, name)
        val fos = FileOutputStream(file)

        val realImage = BitmapFactory.decodeByteArray(data, 0, data.size)
        val exif = ExifInterface(file.toString())
        val orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION)!!.toLowerCase()

        // todo заменить `magic numbers` на именные константы
        val degrees = when(orientation) {
            "6", "0" -> 90
            "8" -> 270
            "3" -> 180
            else -> 0
        }

        val width = realImage.width
        val height = realImage.height
        Timber.i("size ${width}x$height")
        val matrix = Matrix()
        matrix.setRotate(degrees.toFloat())

        val rotatedImage = Bitmap.createBitmap(realImage, 0, 0, width, height, matrix, true)

        try {
            if (!rotatedImage.compress(Bitmap.CompressFormat.JPEG, 55, fos)) {
                throw IOException()
            }
            return mediaStoreInsert(file.path, name, width, height)
        }
        finally {
            realImage.recycle()
            rotatedImage.recycle()
            fos.write(data)
            fos.close()
        }
    }

    private fun mediaStoreInsert(path: String, name: String, width: Int, height: Int): Int {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, name)
        values.put(MediaStore.Images.ImageColumns.BUCKET_ID, context.packageName)
        values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, context.getString(R.string.app_name))
        values.put(MediaStore.Images.ImageColumns.WIDTH, width)
        values.put(MediaStore.Images.ImageColumns.HEIGHT, height)
        values.put(MediaStore.Images.ImageColumns.DATA, path)
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!

        var cursor: Cursor? = null

        try {
            val projection = arrayOf(MediaStore.Images.ImageColumns._ID)
            cursor = context.contentResolver
                .query(uri, projection,
                    null, null, null)?.apply {
                    moveToFirst()
                    return getInt(
                        getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID))
                }
        }
        finally {
            cursor?.close()
        }

        throw IOException()
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