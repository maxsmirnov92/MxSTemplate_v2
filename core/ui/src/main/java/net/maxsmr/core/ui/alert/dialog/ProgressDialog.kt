package net.maxsmr.core.ui.alert.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import androidx.core.view.isVisible
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.R

class ProgressDialog(
    context: Context,
    alert: Alert,
    private val cancelable: Boolean,
    private val dimBackground: Boolean,
    private val onCancel: (() -> Unit)? = null
) : BaseCustomDialog(context, 0, R.layout.dialog_progress, alert) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (dimBackground) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        val title = findViewById<TextView>(R.id.tvTitle)
        val text = alert.title?.get(context) ?: alert.message?.get(context)
        title.text = text
        title.isVisible = text != null

        setCancelable(cancelable)
        setCanceledOnTouchOutside(cancelable)
        onCancel?.let { setOnCancelListener { it() } }
    }
}