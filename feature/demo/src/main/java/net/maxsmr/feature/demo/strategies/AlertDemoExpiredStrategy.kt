package net.maxsmr.feature.demo.strategies

import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.alert.representation.asOkDialog
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.demo.R
import kotlin.system.exitProcess

class AlertDemoExpiredStrategy(
    private val viewModel: BaseViewModel,
    private val fragment: BaseVmFragment<*>,
    private val messageArg: String? = null,
    private val confirmAction: ConfirmAction? = ConfirmAction.FINISH_ACTIVITY,
): IDemoExpiredStrategy {

    init {
        fragment.bindAlertDialog(DIALOG_TAG_DEMO_EXPIRED) {
            it.asOkDialog(fragment.requireContext(), cancelable = false)
        }
    }

    override fun doAction() {
        viewModel.showOkDialog(DIALOG_TAG_DEMO_EXPIRED,
            if (messageArg != null) {
                TextMessage(R.string.demo_period_expired_message_format, messageArg)
            } else {
                TextMessage(R.string.demo_period_expired_message)
            }
        ) {
            when(confirmAction) {
                ConfirmAction.FINISH_ACTIVITY -> {
                    fragment.requireActivity().finish()
                }
                ConfirmAction.EXIT_PROCESS -> {
                    exitProcess(0)
                }
                else -> {

                }
            }
        }
    }

    enum class ConfirmAction {
        FINISH_ACTIVITY,
        EXIT_PROCESS,
    }

    companion object {

        private const val DIALOG_TAG_DEMO_EXPIRED = "demo_expired"
    }
}