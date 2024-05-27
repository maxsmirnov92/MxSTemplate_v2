package net.maxsmr.core.ui.views.snackbar

import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.actions.SnackbarAction.SnackbarLength
import net.maxsmr.core.ui.R

fun View.showSnackbar(
    message: TextMessage,
    length: SnackbarLength,
    callback: BaseTransientBottomBar.BaseCallback<Snackbar>? = null,
): Snackbar {
    check(length != SnackbarLength.INDEFINITE) {
        "Snackbar length cannot be INDEFINITE for that case"
    }
    val snackbar = createSnackbar(message, length, callback)
    snackbar.show()
    return snackbar
}

fun View.createSnackbar(
    message: TextMessage,
    length: SnackbarLength,
    callback: BaseTransientBottomBar.BaseCallback<Snackbar>? = null,
): Snackbar {

    val snackbar = Snackbar.make(this, message.get(context), length.value)
    snackbar.isGestureInsetBottomIgnored = true

    val snackbarView = snackbar.view
    snackbarView.fitsSystemWindows = false
    snackbarView.setBackgroundColor(ContextCompat.getColor(context, R.color.snackbarBackground))

    val snackbarTextView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
    snackbarTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimensionPixelSize(R.dimen.snackbarMessageTextSize).toFloat())

    callback?.let {
        snackbar.addCallback(callback)
    }

    // убираем возможность закрыть снэкбар свайпом, которая появляется автоматически, если снэкбар находится в CoordinatorLayout
    snackbarView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            val lp = snackbarView.layoutParams
            if (lp is CoordinatorLayout.LayoutParams) {
                lp.behavior = DisableSwipeBehavior()
                snackbarView.layoutParams = lp
            }
            snackbarView.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    })

    return snackbar
}