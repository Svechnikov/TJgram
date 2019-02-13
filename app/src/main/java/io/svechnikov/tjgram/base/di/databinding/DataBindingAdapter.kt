package io.svechnikov.tjgram.base.di.databinding

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.squareup.picasso.Picasso
import javax.inject.Inject

class DataBindingAdapter @Inject constructor(private val picasso: Picasso) {

    @BindingAdapter("imageUrl")
    fun setImageUrl(imageView: ImageView,
                    url: String?) {

        url?.let {
            picasso.load(url)
                .into(imageView)
        }
    }

    @BindingAdapter("imageUrl", "widthDimen")
    fun setImageUrlWithWidthDimen(imageView: ImageView,
                    url: String?,
                    widthDimen: Int) {

        url?.let {
            val width = imageView.context.resources.getDimensionPixelSize(widthDimen)
            picasso.load(url)
                .resize(width, 0)
                .into(imageView)
        }
    }
}