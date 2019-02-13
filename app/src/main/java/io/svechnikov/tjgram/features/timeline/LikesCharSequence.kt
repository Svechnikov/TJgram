package io.svechnikov.tjgram.features.timeline

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.content.ContextCompat
import io.svechnikov.tjgram.R

class LikesCharSequence(
    private val context: Context,
    private val likes: Int) : SpannableString(likes.toString()) {

    init {
        setSpan(object: ClickableSpan() {

            override fun updateDrawState(ds: TextPaint) {
                ds.color = when {
                    likes < 0 -> ContextCompat.getColor(context, R.color.likeMinus)
                    likes > 0 -> ContextCompat.getColor(context, R.color.likePlus)
                    else -> ContextCompat.getColor(context, R.color.likeNeutral)
                }
                ds.typeface = Typeface.DEFAULT_BOLD
            }

            override fun onClick(widget: View) {

            }
        }, 0, length, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
    }
}