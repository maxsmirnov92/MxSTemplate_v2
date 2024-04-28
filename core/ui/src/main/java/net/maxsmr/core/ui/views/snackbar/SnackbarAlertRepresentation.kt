package net.maxsmr.core.ui.views.snackbar

import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.alert.representation.SnackbarRepresentation
import net.maxsmr.core.ui.alert.representation.toRepresentation

fun Alert.asSnackbar(view: View?): SnackbarRepresentation? {
    view ?: return null
    val message = title ?: message
    check(message != null) {
        "Alert must contain title or message for being displayed as snackbar"
    }
    check(answers.size <= 1) {
        "Alert must contain 0 or 1 answer for being displayed as snackbar"
    }

    val snackbar: Snackbar = Snackbar.make(view, message.get(view.context), Snackbar.LENGTH_INDEFINITE)
    // текст вью с текстом основного сообщения
    val snackbarView = snackbar.view
    val snackbarTextView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
    // текст вью с текстом кнопки действия ( проверить снова в данном случаи )
    val snackbarActionTextView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
    // устнавливаем размер текста кнопки действия в 12sp
    snackbarActionTextView.textSize = 12f
    val font = ResourcesCompat.getFont(view.context, net.maxsmr.designsystem.shared_res.R.font.roboto_light)
    // устанавливаем стиль основного текста
    snackbarTextView.typeface = font
    // устнавливаем размер основного текста в 12sp
    snackbarTextView.textSize = 12f

    answers.getOrNull(0)?.let { answer ->
        snackbar.setAction(answer.title.get(view.context)) {
            answer.select?.invoke()
            close()
        }
        snackbar.setActionTextColor(ContextCompat.getColor(view.context, R.color.snackbarActionColor))
    }
    snackbar.isGestureInsetBottomIgnored = true
    snackbarView.setBackgroundColor(ContextCompat.getColor(view.context, R.color.snackbarBackground))

    snackbarView.fitsSystemWindows = false

    //убираем возможность закрыть снэкбар свайпом, которая появляется автоматически, если снэкбар находится в CoordinatorLayout
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
    return snackbar.toRepresentation()
}