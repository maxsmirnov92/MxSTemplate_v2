package net.maxsmr.core.ui.view.location

import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.ui.location.LocationViewModel
import net.maxsmr.core.ui.location.LocationViewModel.Companion.DIALOG_TAG_LOCATION_NOT_AVAILABLE
import net.maxsmr.core.ui.location.LocationViewModel.Companion.DIALOG_TAG_LOCATION_NOT_ENABLED
import net.maxsmr.core.ui.view.alert.delegate.BaseFragmentViewAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asOkDialog
import net.maxsmr.core.ui.view.alert.representation.asYesNoDialog

class LocationFragmentAlertDelegate(
    override val fragment: BaseVmFragment<*>,
    override val viewModel: LocationViewModel,
): BaseFragmentViewAlertDelegate<LocationViewModel>() {

    override fun handleAlertDialogs() {
        bindAlertDialog(DIALOG_TAG_LOCATION_NOT_AVAILABLE) {
            it.asOkDialog(context, true)
        }
        bindAlertDialog(DIALOG_TAG_LOCATION_NOT_ENABLED) { alert ->
            alert.asYesNoDialog(context, false)
        }
    }
}