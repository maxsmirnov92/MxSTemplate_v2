package net.maxsmr.core.ui.views.snackbar

import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import net.maxsmr.core.ui.R


fun showNoInternetSnackbar(parentView: View, onActionClickListener: View.OnClickListener?): Snackbar {
    return parentView.showNoInternetSnackbar(onActionClickListener, null)
}

/**
 * Метод для показа snackbar - ра об отсутствии интернета
 *
 * @receiver вью родитель снизу которого должно показаться уведомление
 * @param onActionClickListener коллбек, который вызовится при нажатии на "проверить снова"
 */
fun View.showNoInternetSnackbar(onActionClickListener: View.OnClickListener?, callback: BaseTransientBottomBar.BaseCallback<Snackbar?>?): Snackbar {
    return showSnackbar(net.maxsmr.core.android.R.string.error_no_connection, net.maxsmr.core.android.R.string.try_again_internet, onActionClickListener, callback)
}

fun View.showSnackbar(@StringRes message: Int, @StringRes button: Int, onActionClickListener: View.OnClickListener?): Snackbar {
    return showSnackbar(message, button, onActionClickListener, null)
}

fun View.showSnackbar(@StringRes message: Int, @StringRes button: Int, onActionClickListener: View.OnClickListener?, callback: BaseTransientBottomBar.BaseCallback<Snackbar?>?): Snackbar {
    val snackbar = Snackbar.make(this, message, Snackbar.LENGTH_INDEFINITE)
    // текст вью с текстом основного сообщения
    val snackbarView = snackbar.view
    val snackbarTextView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
    // текст вью с текстом кнопки действия ( проверить снова в данном случаи )
    val snackbarActionTextView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
    // устнавливаем размер текста кнопки действия в 12sp
    snackbarActionTextView.textSize = 12f
    val font = ResourcesCompat.getFont(context, net.maxsmr.designsystem.shared_res.R.font.roboto_light)
    // устанавливаем стиль основного текста
    snackbarTextView.setTypeface(font)
    // устнавливаем размер основного текста в 12sp
    snackbarTextView.textSize = 12f
    snackbar.setAction(button, onActionClickListener)
    snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.snackbarActionColor))
    snackbarView.setBackgroundColor(ContextCompat.getColor(context, R.color.snackbarBackground))
    if (callback != null) {
        snackbar.addCallback(callback)
    }
    snackbar.show()
    //убираем возможность закрыть снэкбар свайпом, которая появляется автоматически, если снэкбар находится в CoordinatorLayout
    snackbarView.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
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
