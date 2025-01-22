package net.maxsmr.core.ui.view.location

import androidx.fragment.app.Fragment
import net.maxsmr.core.ui.view.alert.delegate.ViewFragmentAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asOkDialog
import net.maxsmr.core.ui.view.alert.representation.asYesNoDialog
import net.maxsmr.core.ui.location.LocationViewModel
import net.maxsmr.core.ui.location.LocationViewModel.Companion.DIALOG_TAG_GPS_NOT_AVAILABLE
import net.maxsmr.core.ui.location.LocationViewModel.Companion.DIALOG_TAG_GPS_NOT_ENABLED

class LocationFragmentAlertDelegate(
    fragment: Fragment,
    viewModel: LocationViewModel,
): ViewFragmentAlertDelegate<LocationViewModel>(fragment, viewModel) {

    override fun handleCommonAlertDialogs() {
        super.handleCommonAlertDialogs()
            bindAlertDialog(DIALOG_TAG_GPS_NOT_AVAILABLE) {
                it.asOkDialog(context, true)
            }
            bindAlertDialog(DIALOG_TAG_GPS_NOT_ENABLED) { alert ->
                alert.asYesNoDialog(context, false)
            }
    }
}