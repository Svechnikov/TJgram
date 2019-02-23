package io.svechnikov.tjgram.features.timeline.child

import android.graphics.Typeface
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View

class IntroCharSequence(userName: String, intro: String) :
    SpannableString("$userName $intro") {

    init {
        setSpan(object: ClickableSpan() {

            override fun updateDrawState(ds: TextPaint) {
                ds.typeface = Typeface.DEFAULT_BOLD
            }

            override fun onClick(widget: View) {

            }
        }, 0, userName.length, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
    }
}