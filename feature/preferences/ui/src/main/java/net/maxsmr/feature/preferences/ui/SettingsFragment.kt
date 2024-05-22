package net.maxsmr.feature.preferences.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.NavArgs
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.commonutils.conversion.toIntNotNull
import net.maxsmr.commonutils.gui.bindTo
import net.maxsmr.commonutils.live.field.observeFrom
import net.maxsmr.core.android.base.alert.AlertHandler
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.ui.alert.representation.asYesNoNeutralDialog
import net.maxsmr.core.ui.bindHintError
import net.maxsmr.core.ui.bindValue
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.toggleFieldState
import net.maxsmr.feature.preferences.ui.databinding.FragmentSettingsBinding
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject
import kotlin.reflect.KClass

@AndroidEntryPoint
class SettingsFragment: BaseNavigationFragment<SettingsViewModel, NavArgs>() {

    override val argsClass: KClass<NavArgs>? = null

    override val layoutId: Int = R.layout.fragment_settings

    override val viewModel: SettingsViewModel by viewModels()

    override val menuResId: Int = R.menu.menu_settings

    private val binding by viewBinding(FragmentSettingsBinding::bind)

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    private var saveMenuItem: MenuItem? = null

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        saveMenuItem = menu.findItem(R.id.actionSave)
        refreshSaveMenuItem(viewModel.hasChanges.value == true)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return if (menuItem.itemId == R.id.actionSave) {
            viewModel.saveChanges()
            true
        } else {
            super.onMenuItemSelected(menuItem)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: SettingsViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)

        binding.etMaxDownloads.bindTo(viewModel.maxDownloadsField) {
            it.toIntNotNull()
        }
        viewModel.maxDownloadsField.observeFrom(binding.etMaxDownloads, viewLifecycleOwner) {
            it.toString()
        }
        viewModel.maxDownloadsField.bindHintError(viewLifecycleOwner, binding.tilMaxDownloads)

        viewModel.retryDownloadsField.bindValue(viewLifecycleOwner, binding.cbRetryDownloads)
        viewModel.disableNotificationsField.bindValue(viewLifecycleOwner, binding.cbDisableNotifications)

        binding.etUpdateNotificationInterval.addTextChangedListener {
            viewModel.updateNotificationIntervalStateField.toggleFieldState(it.toString().toLongOrNull() ?: 0)
        }
        viewModel.updateNotificationIntervalStateField.observeFrom(binding.etUpdateNotificationInterval, viewLifecycleOwner) {
            binding.etUpdateNotificationInterval.isEnabled = it.isEnabled
            it.value.toString()
        }
        viewModel.updateNotificationIntervalStateField.bindHintError(viewLifecycleOwner, binding.tilUpdateNotificationInterval)

        viewModel.hasChanges.observe {
            refreshSaveMenuItem(it)
        }
    }

    override fun handleAlerts() {
        super.handleAlerts()
        bindAlertDialog(SettingsViewModel.DIALOG_TAG_CONFIRM_EXIT) {
            it.asYesNoNeutralDialog(requireContext())
        }
    }

    override fun onUpPressed(): Boolean {
        viewModel.navigateBackWithAlert()
        return true
    }

    override fun onBackPressed() {
        viewModel.navigateBackWithAlert()
    }

    private fun refreshSaveMenuItem(isEnabled: Boolean) {
        saveMenuItem?.isEnabled = isEnabled
    }
}