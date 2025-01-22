package net.maxsmr.core.ui.alert.representation

import android.content.Context
import net.maxsmr.commonutils.ISpanInfo
import net.maxsmr.commonutils.createSpanText
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.alert.Alert

abstract class BaseDialogAlertRepresentationBuilder<AR: AlertRepresentation>(protected val alert: Alert) {

    protected var titleSpans: List<ISpanInfo> = emptyList()
    protected var messageSpans: List<ISpanInfo> = emptyList()

    protected var cancelable = true
    protected var onCancel: (() -> Unit)? = null

    protected var positiveAnswer: Alert.Answer? = null
    protected var onPositiveClick: (() -> Unit)? = null

    protected var negativeAnswer: Alert.Answer? = null
    protected var onNegativeClick: (() -> Unit)? = null

    protected var neutralAnswer: Alert.Answer? = null
    protected var onNeutralClick: (() -> Unit)? = null
    
    protected var onDismiss: (() -> Unit)? = null

    abstract fun build(): AR

    protected fun TextMessage.format(context: Context, spans: List<ISpanInfo>): CharSequence {
        return get(context).createSpanText(*spans.toTypedArray())
    }
}