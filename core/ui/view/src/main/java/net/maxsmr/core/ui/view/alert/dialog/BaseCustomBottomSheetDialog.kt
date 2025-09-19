package net.maxsmr.core.ui.view.alert.dialog

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import net.maxsmr.commonutils.convertAnyToPx
import net.maxsmr.commonutils.getStatusBarHeight
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.Alert.Answer.Companion.findByTag
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.view.databinding.LayoutBottomSheetDialogBaseBinding

abstract class BaseCustomBottomSheetDialog(
    context: Context,
    themeResId: Int = R.style.BottomSheetDialogTheme,
    @LayoutRes val layoutResId: Int,
    val alert: Alert? = null,
    private val cancelable: Boolean = true,
) : BottomSheetDialog(context, themeResId), DialogInterface.OnCancelListener {

    protected lateinit var wrappedContentView: View
        private set
    private lateinit var baseContentView: View

    private val mainContentBinding by lazy {
        LayoutBottomSheetDialogBaseBinding.bind(baseContentView)
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(LayoutInflater.from(context)) {
            baseContentView = inflate(net.maxsmr.core.ui.view.R.layout.layout_bottom_sheet_dialog_base, null, false)
            wrappedContentView = (inflate(layoutResId, mainContentBinding.container, true) as ViewGroup).getChildAt(0)
        }
        setContentView(baseContentView)

        with(mainContentBinding.ivClose) {
            val closeAnswer = alert?.answers?.findByTag(ANSWER_TAG_CLOSE)
            isVisible = alert == null || closeAnswer != null
            setOnClickListener {
                onClose()
                closeAnswer?.select?.invoke()
            }
        }

        setCancelable(cancelable)
        super.setOnCancelListener(this)
        setOnShowListener(null)
    }

    override fun setOnShowListener(listener: DialogInterface.OnShowListener?) {
        super.setOnShowListener { dialog ->
            val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
            onSetupBehaviour(BottomSheetBehavior.from(bottomSheet))
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

    @CallSuper
    protected open fun onSetupBehaviour(behavior: BottomSheetBehavior<out View>) {
        val res = context.resources
        val height = (
                res.displayMetrics.heightPixels
                        - baseContentView.getStatusBarHeight()
                        - res.convertAnyToPx(40f).toInt()
                )

        behavior.maxHeight = height
        behavior.peekHeight = height

        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
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

        const val ANSWER_TAG_CLOSE = "close"
    }
}