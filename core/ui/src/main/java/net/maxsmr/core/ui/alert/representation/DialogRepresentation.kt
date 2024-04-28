package net.maxsmr.core.ui.alert.representation

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.BadTokenException
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import net.maxsmr.commonutils.ISpanInfo
import net.maxsmr.commonutils.createSpanText
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.representation.AlertRepresentation
import net.maxsmr.core.ui.alert.representation.DialogRepresentation.Builder.MultiChoiceAnswersData.Companion.isNotEmpty

fun Dialog.toRepresentation(): DialogRepresentation = DialogRepresentation(this)

class DialogRepresentation(
    private val dialog: Dialog,
) : AlertRepresentation {

    fun configure(block: (Dialog) -> Unit) = apply {
        dialog.setOnShowListener { block(dialog) }
    }

    override fun show() {
        dialog.show()
    }

    override fun hide() {
        try {
            dialog.dismiss()
        } catch (e: Exception) {
        }
    }

    class Builder(
        val context: Context,
        val alert: Alert,
    ) {

        @StyleRes
        private var themeResId: Int? = null

        private var customView: View? = null

        @LayoutRes
        private var customViewResId: Int? = null
        private var customViewConfigBlock: (Dialog.() -> Unit)? = null

        private var titleSpans: List<ISpanInfo> = emptyList()
        private var messageSpans: List<ISpanInfo> = emptyList()

        private var cancelable = true
        private var onCancel: (() -> Unit)? = null

        private var positiveAnswer: Alert.Answer? = null
        private var onPositiveClick: (() -> Unit)? = null

        private var negativeAnswer: Alert.Answer? = null
        private var onNegativeClick: (() -> Unit)? = null

        private var neutralAnswer: Alert.Answer? = null
        private var onNeutralClick: (() -> Unit)? = null

        private var multiChoiceAnswers: MultiChoiceAnswersData? = null
        private var onMultiChoiceClick: ((Int) -> Unit)? = null

        private var onDismiss: (() -> Unit)? = null

        fun setThemeResId(@StyleRes themeResId: Int) = apply {
            this.themeResId = themeResId
        }

        @JvmOverloads
        fun setCustomView(customView: View, config: (Dialog.() -> Unit)? = null) = apply {
            this.customView = customView
            this.customViewConfigBlock = config
        }

        @JvmOverloads
        fun setCustomView(@LayoutRes customViewResId: Int, config: (Dialog.() -> Unit)? = null) = apply {
            this.customViewResId = customViewResId
            this.customViewConfigBlock = config
        }

        fun setTitleSpans(vararg spans: ISpanInfo?) = apply {
            this.titleSpans = spans.filterNotNull()
        }

        fun setMessageSpans(vararg spans: ISpanInfo?) = apply {
            this.messageSpans = spans.filterNotNull()
        }

        fun setCancelable(cancelable: Boolean) = apply {
            this.cancelable = cancelable
        }

        fun setOnCancelListener(onCancel: () -> Unit) = apply {
            this.onCancel = onCancel
        }

        fun setPositiveButton(answer: Alert.Answer, onClick: (() -> Unit)? = null) = apply {
            positiveAnswer = answer
            onPositiveClick = onClick
        }

        fun setNegativeButton(answer: Alert.Answer, onClick: (() -> Unit)? = null) = apply {
            negativeAnswer = answer
            onNegativeClick = onClick
        }

        fun setNeutralButton(answer: Alert.Answer, onClick: (() -> Unit)? = null) = apply {
            neutralAnswer = answer
            onNeutralClick = onClick
        }

        fun setMultiChoiceAnswers(answers: MultiChoiceAnswersData, onClick: ((index: Int) -> Unit)? = null) = apply {
            multiChoiceAnswers = answers
            onMultiChoiceClick = onClick
        }

        fun setOnDismiss(onDismiss: () -> Unit) = apply {
            this.onDismiss = onDismiss
        }

        fun build(): DialogRepresentation {
            val hasAnyAnswer = positiveAnswer != null
                    || negativeAnswer != null
                    || neutralAnswer != null
                    || multiChoiceAnswers.isNotEmpty()
            check(hasAnyAnswer || cancelable || customView != null || customViewResId != null) {
                "Cannot create non cancelable dialog without answers. How to dismiss it?"
            }
            return (themeResId?.let {
                AppAlertDialogBuilder(
                    context,
                    it
                )
            } ?: AppAlertDialogBuilder(context))
                .apply {
                    alert.title?.format(context, titleSpans)?.let(this::setTitle)
                    alert.message?.format(context, messageSpans)?.let(this::setMessage)
                    positiveAnswer?.let {
                        setPositiveButton(it.title.get(context)) { _, _ ->
                            it.select?.invoke()
                            onPositiveClick?.invoke()
                            onDismiss?.invoke()
                        }
                    }
                    negativeAnswer?.let {
                        setNegativeButton(it.title.get(context)) { _, _ ->
                            it.select?.invoke()
                            onNegativeClick?.invoke()
                            onDismiss?.invoke()
                        }
                    }
                    neutralAnswer?.let {
                        setNeutralButton(it.title.get(context)) { _, _ ->
                            it.select?.invoke()
                            onNeutralClick?.invoke()
                            onDismiss?.invoke()
                        }
                    }
                    val multiChoiceAnswers = multiChoiceAnswers
                    if (multiChoiceAnswers != null && multiChoiceAnswers.isNotEmpty()) {
                        val adapter = object : ArrayAdapter<CharSequence>(
                            context,
                            multiChoiceAnswers.dialogItemResId,
                            multiChoiceAnswers.itemViewResId,
                            multiChoiceAnswers.answers.map { it.title.get(context) }) {

                            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                                val view = super.getView(position, convertView, parent)
                                val itemView = view.findViewById<TextView>(multiChoiceAnswers.itemViewResId)
                                itemView.contentDescription =
                                    multiChoiceAnswers.answers[position].contentDescription?.get(context)
                                        ?: itemView.text
                                return view
                            }
                        }
                        setAdapter(adapter) { _, which ->
                            multiChoiceAnswers.answers[which].select?.invoke()
                            onMultiChoiceClick?.invoke(which)
                            onDismiss?.invoke()
                        }
                    }
                    customView?.let { setView(it) }
                    customViewResId?.let { setView(it) }
                }
                .setCancelable(cancelable)
                .setOnCancelListener {
                    alert.close()
                    onCancel?.invoke()
                    onDismiss?.invoke()
                }
                .create()
                .also { dialog ->
                    dialog.setCanceledOnTouchOutside(cancelable)
                    customViewConfigBlock?.let { config ->
                        dialog.setOnShowListener { dialog.config() }
                    }
                }
                .toRepresentation()
        }

        private fun TextMessage.format(context: Context, spans: List<ISpanInfo>): CharSequence {
            return get(context).createSpanText(*spans.toTypedArray())
        }

        data class MultiChoiceAnswersData(
            val answers: List<Alert.Answer> = emptyList(),
            @LayoutRes val dialogItemResId: Int,
            @IdRes val itemViewResId: Int
        ) {

            val isEmpty = answers.isEmpty()

            companion object {

                fun MultiChoiceAnswersData?.isNotEmpty() = this != null && !this.isEmpty
            }
        }
    }


    private class AppAlertDialogBuilder(context: Context, themeResId: Int = 0) : AlertDialog.Builder(context, themeResId) {

        var dialog: AlertDialog? = null
            private set

        override fun show(): AlertDialog? {
            dialog = null
            try {
                with(create()) {
                    dialog = this
                    this.show()
                    setButtonGravity(this.getButton(DialogInterface.BUTTON_POSITIVE))
                    setButtonGravity(this.getButton(DialogInterface.BUTTON_NEGATIVE))
                    setButtonGravity(this.getButton(DialogInterface.BUTTON_NEUTRAL))
                }
            } catch (ignored: BadTokenException) {
            }
            return dialog
        }

        private fun setButtonGravity(button: Button?) {
            if (button != null) {
                button.gravity = Gravity.END
            }
        }
    }


}