package net.maxsmr.core.ui.alert.representation

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import net.maxsmr.commonutils.getActionBarHeight
import net.maxsmr.commonutils.getStatusBarHeight
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.R


abstract class BaseCustomBottomSheetDialog(
    context: Context,
    themeResId: Int = R.style.BottomSheetDialogTheme,
    @LayoutRes val layoutResId: Int,
    val alert: Alert? = null,
    private val shouldMatchHeight: Boolean = true,
) : BottomSheetDialog(context, themeResId), DialogInterface.OnCancelListener {

    protected lateinit var mainContentView: View
        private set
    protected lateinit var wrappedContentView: View
        private set

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(LayoutInflater.from(context)) {
            mainContentView = inflate(R.layout.layout_bottom_sheet_dialog, null, false)
            val container = mainContentView.findViewById<ViewGroup>(R.id.container)
            wrappedContentView = (inflate(layoutResId, container, true) as ViewGroup).getChildAt(0)
        }
        setContentView(mainContentView)
        mainContentView.findViewById<View>(R.id.ivClose).setOnClickListener {
            onClose()
        }

        super.setOnCancelListener(this)
        setOnShowListener(null)
    }

    override fun setOnShowListener(listener: DialogInterface.OnShowListener?) {
        super.setOnShowListener { dialog ->
            (dialog as? Dialog)?.setExpanded()
            listener?.onShow(dialog)
        }
    }

    override fun setOnCancelListener(listener: DialogInterface.OnCancelListener?) {
        super.setOnCancelListener {
            onCancel(it)
            listener?.onCancel(it)
        }
    }

    @CallSuper
    override fun onCancel(dialog: DialogInterface?) {
        alert?.close()
    }

    @CallSuper
    protected fun onClose() {
        alert?.close()
    }

    protected fun setOnAnswerCancelListener(answer: Alert.Answer?, onCancel: (() -> Unit)? = null) {
        this.setOnCancelListener {
            answer?.select?.invoke()
            onCancel?.invoke()
        }
    }

    protected fun View.setOnAnswerClickListener(answer: Alert.Answer?, onClick: View.OnClickListener? = null) {
        this.setOnClickListener {
            answer?.select?.invoke()
            onClick?.onClick(it)
        }
    }

    companion object {

        fun Dialog.setExpanded() {
            val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
            val height = (bottomSheet.resources.displayMetrics.heightPixels
                    - getActionBarHeight(bottomSheet.context)
                    - if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) getStatusBarHeight(bottomSheet.context) else 0)
            BottomSheetBehavior.from(bottomSheet).peekHeight = height
            bottomSheet.updateLayoutParams {
                this.height = height
            }
        }
    }
}