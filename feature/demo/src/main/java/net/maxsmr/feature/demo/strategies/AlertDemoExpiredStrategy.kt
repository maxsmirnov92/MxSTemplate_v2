package net.maxsmr.feature.demo.strategies

import android.app.Activity
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.ui.view.alert.delegate.BaseFragmentViewAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asOkDialog
import net.maxsmr.feature.demo.R
import kotlin.system.exitProcess

class AlertDemoExpiredStrategy(
    private val viewModel: BaseViewModel,
    private val activity: Activity,
    private val messageArg: String? = null,
    private val confirmAction: ConfirmAction? = ConfirmAction.FINISH_ACTIVITY,
) : IDemoExpiredStrategy {

    override fun doAction() {
        viewModel.showOkDialog(
            DIALOG_TAG_DEMO_EXPIRED,
            if (messageArg != null) {
                TextMessage(R.string.demo_period_expired_message_format, messageArg)
            } else {
                TextMessage(R.string.demo_period_expired_message)
            }
        ) {
            when (confirmAction) {
                ConfirmAction.FINISH_ACTIVITY -> {
                    activity.finish()
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

    class DemoViewFragmentAlertDelegate<VM : BaseViewModel>(
        override val fragment: BaseVmFragment<VM>,
        override val viewModel: VM,
    ) : BaseFragmentViewAlertDelegate<VM>() {

        override fun handleAlertDialogs() {
            bindAlertDialog(DIALOG_TAG_DEMO_EXPIRED) {
                it.asOkDialog(fragment.requireContext(), cancelable = false)
            }
        }
    }

    companion object {

        const val DIALOG_TAG_DEMO_EXPIRED = "demo_expired"
    }
}