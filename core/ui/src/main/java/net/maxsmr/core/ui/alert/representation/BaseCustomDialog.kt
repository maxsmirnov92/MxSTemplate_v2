package net.maxsmr.core.ui.alert.representation

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueue

/**
 * Абстракция для отображения [Alert] в диалога с кастомным лейаутом.
 * Предоставляет методы [setOnAnswerClickListener] для выбора ответа на алерт и обертку
 * [setOnCancelListener] для удаления алерта из [AlertQueue].
 *
 * Для не кастомных диалогов см. [DialogRepresentation.Builder]
 */
abstract class BaseCustomDialog(
    context: Context,
    themeResId: Int,
    @LayoutRes val layoutRes: Int,
    val alert: Alert,
) : Dialog(context, themeResId), DialogInterface.OnCancelListener {

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutRes)
        super.setOnCancelListener(this)
    }

    override fun setOnCancelListener(listener: DialogInterface.OnCancelListener?) {
        super.setOnCancelListener {
            onCancel(it)
            listener?.onCancel(it)
        }
    }

    override fun onCancel(dialog: DialogInterface?) {
        alert.close()
    }

    protected fun View.setOnAnswerClickListener(answer: Alert.Answer, onClick: View.OnClickListener?) {
        this.setOnClickListener {
            answer.select?.invoke()
            onClick?.onClick(it)
        }
    }
}