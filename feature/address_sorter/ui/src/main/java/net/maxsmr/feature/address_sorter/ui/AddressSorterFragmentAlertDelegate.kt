package net.maxsmr.feature.address_sorter.ui

import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.ui.view.alert.delegate.BaseFragmentViewAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asMultiChoiceDialog
import net.maxsmr.core.ui.view.alert.representation.asOkDialog
import net.maxsmr.core.ui.view.alert.representation.asYesNoDialog
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.Companion.DIALOG_TAG_CHANGE_ROUTING_MODE
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.Companion.DIALOG_TAG_CHANGE_ROUTING_TYPE
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.Companion.DIALOG_TAG_CHANGE_SORT_PRIORITY
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.Companion.DIALOG_TAG_CLEAR_ITEMS
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.Companion.DIALOG_TAG_DOWNLOAD_KEY_FAILED
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.Companion.DIALOG_TAG_EXPORT_FAILED
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.Companion.DIALOG_TAG_EXPORT_SUCCESS
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.Companion.DIALOG_TAG_IMPORT_FAILED
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.Companion.DIALOG_TAG_REVERSE_GEOCODE_FAILED
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.Companion.DIALOG_TAG_ROUTING_FAILED

class AddressSorterFragmentAlertDelegate(
    override val fragment: BaseVmFragment<AddressSorterViewModel>,
    override val viewModel: AddressSorterViewModel,
) : BaseFragmentViewAlertDelegate<AddressSorterViewModel>() {

    override fun handleAlertDialogs() {
        bindAlertDialog(DIALOG_TAG_IMPORT_FAILED) {
            it.asOkDialog(context)
        }
        bindAlertDialog(DIALOG_TAG_EXPORT_SUCCESS) {
            it.asOkDialog(context)
        }
        bindAlertDialog(DIALOG_TAG_EXPORT_FAILED) {
            it.asOkDialog(context)
        }
        bindAlertDialog(DIALOG_TAG_CHANGE_ROUTING_MODE) {
            it.asMultiChoiceDialog(context, isRadioButton = true)
        }
        bindAlertDialog(DIALOG_TAG_CHANGE_ROUTING_TYPE) {
            it.asMultiChoiceDialog(context, isRadioButton = true)
        }
        bindAlertDialog(DIALOG_TAG_CHANGE_SORT_PRIORITY) {
            it.asMultiChoiceDialog(context, isRadioButton = true)
        }
        bindAlertDialog(DIALOG_TAG_CLEAR_ITEMS) {
            it.asYesNoDialog(context)
        }
        bindAlertDialog(DIALOG_TAG_REVERSE_GEOCODE_FAILED) {
            it.asOkDialog(context)
        }
        bindAlertDialog(DIALOG_TAG_ROUTING_FAILED) {
            it.asOkDialog(context)
        }
        bindAlertDialog(DIALOG_TAG_DOWNLOAD_KEY_FAILED) {
            it.asOkDialog(context)
        }
    }
}