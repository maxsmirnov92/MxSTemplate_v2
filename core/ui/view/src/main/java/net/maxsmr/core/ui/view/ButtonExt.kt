package net.maxsmr.core.ui.view

import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.android.material.button.MaterialButton

fun MaterialButton.setShowProgress(
    toggle: Boolean,
    @ColorRes colorResId: Int? = null,
    @DrawableRes defaultDrawableResId: Int? = null
) {
    icon = if (toggle) {
        CircularProgressDrawable(context).apply {
            setStyle(CircularProgressDrawable.DEFAULT)
            setColorSchemeColors(ContextCompat.getColor(context, colorResId ?: net.maxsmr.designsystem.shared_res.R.color.colorAccent))
            start()
        }
    } else {
        defaultDrawableResId?.let {
            ContextCompat.getDrawable(context, it)
        }
    }?.also {
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