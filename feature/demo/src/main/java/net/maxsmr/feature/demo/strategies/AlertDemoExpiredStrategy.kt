package net.maxsmr.feature.demo.strategies

import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.alert.representation.asOkDialog
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.demo.R

class AlertDemoExpiredStrategy(
    private val viewModel: BaseViewModel,
    private val fragment: BaseVmFragment<*>,
    private val messageArg: String? = null,
    private val navigateBackOnDismiss: Boolean = true,
): IDemoExpiredStrategy {

    init {
        fragment.bindAlertDialog(DIALOG_TAG_DEMO_EXPIRED) { it.asOkDialog(fragment.requireContext()) }
    }

    override fun doAction() {
        viewModel.showOkDialog(DIALOG_TAG_DEMO_EXPIRED,
            if (messageArg != null) {
                TextMessage(R.string.demo_period_expired_message_format, messageArg)
            } else {
                TextMessage(R.string.demo_period_expired_message)
            }
        ) {
            if (navigateBackOnDismiss) {
                viewModel.navigateBack()
            }
        }
    }

    companion object {

        private const val DIALOG_TAG_DEMO_EXPIRED = "demo_expired"
    }
}