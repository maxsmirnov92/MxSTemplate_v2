package net.maxsmr.core.android.base.actions

import com.google.android.material.snackbar.Snackbar
import net.maxsmr.commonutils.gui.message.TextMessage
import java.io.Serializable

@Deprecated("use snackbarQueue")
class SnackbarAction(
    private val message: TextMessage,
    private val data: SnackbarExtraData,
) : BaseViewModelAction<SnackbarAction.ISnackbarActor>() {

    override fun doAction(actor: ISnackbarActor) {
        actor.showSnackbar(message, data)
    }

    interface ISnackbarActor {

        fun showSnackbar(message: TextMessage, data: SnackbarExtraData)
    }
}

data class SnackbarExtraData(
    val length: SnackbarLength = SnackbarLength.SHORT,
    val maxLines: Int? = 4,
) : Serializable {

    enum class SnackbarLength(val value: Int) {

        INDEFINITE(Snackbar.LENGTH_INDEFINITE),
        SHORT(Snackbar.LENGTH_SHORT),
        LONG(Snackbar.LENGTH_LONG)
    }
}