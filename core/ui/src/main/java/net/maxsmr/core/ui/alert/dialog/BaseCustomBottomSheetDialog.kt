package net.maxsmr.core.ui.alert.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.StableState
import com.google.android.material.bottomsheet.BottomSheetDialog
import net.maxsmr.commonutils.getActionBarHeight
import net.maxsmr.commonutils.getStatusBarHeight
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.Alert.Answer.Companion.findByTag
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.databinding.LayoutBottomSheetDialogBaseBinding

abstract class BaseCustomBottomSheetDialog(
    context: Context,
    themeResId: Int = R.style.BottomSheetDialogTheme,
    @LayoutRes val layoutResId: Int,
    val alert: Alert? = null,
    private val cancelable: Boolean = true,
    private val shouldMatchHeight: Boolean = true,
    private val initialState: BottomSheetState = BottomSheetState.STATE_EXPANDED,
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
            baseContentView = inflate(R.layout.layout_bottom_sheet_dialog_base, null, false)
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

        val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
        BottomSheetBehavior.from(bottomSheet).onSetupBehaviour()
    }

    override fun setOnShowListener(listener: DialogInterface.OnShowListener?) {
        super.setOnShowListener { dialog ->
            if (shouldMatchHeight) {
                (dialog as? Dialog)?.setExpanded()
            }
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
    protected fun BottomSheetBehavior<out View>.onSetupBehaviour() {
        state = this@BaseCustomBottomSheetDialog.initialState.value
//        peekHeight
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

    enum class BottomSheetState(@StableState val value: Int) {
        STATE_EXPANDED(BottomSheetBehavior.STATE_EXPANDED),
        STATE_COLLAPSED(BottomSheetBehavior.STATE_COLLAPSED),
        STATE_HIDDEN(BottomSheetBehavior.STATE_HIDDEN),
        STATE_HALF_EXPANDED(BottomSheetBehavior.STATE_HALF_EXPANDED)
    }

    companion object {

        const val ANSWER_TAG_CLOSE = "close"

        fun Dialog.setExpanded() {
            val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
            val height = (bottomSheet.resources.displayMetrics.heightPixels
                    - bottomSheet.context.getActionBarHeight()
                    - if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) bottomSheet.resources.getStatusBarHeight() else 0)
            BottomSheetBehavior.from(bottomSheet).peekHeight = height
            bottomSheet.updateLayoutParams {
                this.height = height
            }
        }
    }
}