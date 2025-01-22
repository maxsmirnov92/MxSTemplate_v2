package net.maxsmr.core.ui.view.alert.delegate

import androidx.fragment.app.Fragment
import net.maxsmr.core.android.base.BaseViewModel

class CombinedViewFragmentAlertDelegate<VM: BaseViewModel>(
    private val delegates: List<ViewFragmentAlertDelegate<VM>>,
    fragment: Fragment,
    viewModel: VM
): ViewFragmentAlertDelegate<VM>(fragment, viewModel) {

    override fun handleCommonAlertDialogs() {
        delegates.forEach {
            it.handleCommonAlertDialogs()
        }
    }

    override fun handleSnackbarAlerts() {
        delegates.forEach {
            it.handleSnackbarAlerts()
        }
    }

    override fun handleToastAlerts() {
        delegates.forEach {
            it.handleToastAlerts()
        }
    }
}