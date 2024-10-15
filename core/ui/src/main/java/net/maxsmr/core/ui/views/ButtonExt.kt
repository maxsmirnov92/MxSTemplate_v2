package net.maxsmr.core.ui.views

import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.android.material.button.MaterialButton
import net.maxsmr.core.ui.R

fun MaterialButton.setShowProgress(toggle: Boolean, @ColorRes colorResId: Int? = null) {
    icon = if (toggle) {
        CircularProgressDrawable(context).apply {
            setStyle(CircularProgressDrawable.DEFAULT)
            setColorSchemeColors(ContextCompat.getColor(context, colorResId ?: R.color.colorAccent))
            start()
        }
    } else {
        null
    }
    icon?.let { // callback to redraw button icon
        it.callback = object : Drawable.Callback {
            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            }

            override fun invalidateDrawable(who: Drawable) {
                this@setShowProgress.invalidate()
            }

            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
            }
        }
    }
}