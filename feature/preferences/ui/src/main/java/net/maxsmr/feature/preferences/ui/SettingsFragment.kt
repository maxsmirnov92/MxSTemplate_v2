package net.maxsmr.feature.preferences.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.commonutils.conversion.toIntNotNull
import net.maxsmr.commonutils.conversion.toLongNotNull
import net.maxsmr.commonutils.gui.bindTo
import net.maxsmr.commonutils.gui.bindToTextNotNull
import net.maxsmr.commonutils.gui.scrollToView
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.live.field.observeFrom
import net.maxsmr.commonutils.live.field.observeFromText
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.fields.bindHintError
import net.maxsmr.core.ui.fields.bindValue
import net.maxsmr.core.ui.fields.toggleFieldState
import net.maxsmr.feature.preferences.ui.databinding.FragmentSettingsBinding
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : BaseNavigationFragment<SettingsViewModel>() {

    override val layoutId: Int = R.layout.fragment_settings

    override val viewModel: SettingsViewModel by viewModels()

    override val menuResId: Int = R.menu.menu_settings

    private val binding by viewBinding(FragmentSettingsBinding::bind)

    private val fieldViewsMap: Map<Field<*>, View> by lazy {
        mutableMapOf<Field<*>, View>().apply {
            with(viewModel) {
                put(maxDownloadsField, binding.tilMaxDownloads)
                put(connectTimeoutField, binding.tilConnectTimeout)
                put(updateNotificationIntervalStateField, binding.tilUpdateNotificationInterval)
                put(startPageUrlField, binding.tilStartPageUrl)

            }
        }
    }

    private val errorFieldFunc = { field: Field<*> ->
        fieldViewsMap[field]?.let {
            binding.svSettings.scrollToView(it, true, activity = requireActivity())
        }
    }

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
            viewModel.saveChanges(errorFieldFunc)
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

        binding.etConnectTimeout.bindTo(viewModel.connectTimeoutField) {
            it.toLongNotNull()
        }
        viewModel.connectTimeoutField.observeFrom(binding.etConnectTimeout, viewLifecycleOwner) {
            it.toString()
        }
        viewModel.connectTimeoutField.bindHintError(viewLifecycleOwner, binding.tilConnectTimeout)

        viewModel.retryDownloadsField.bindValue(viewLifecycleOwner, binding.switchRetryDownloads)
        viewModel.disableNotificationsField.bindValue(viewLifecycleOwner, binding.switchDisableNotifications)

        binding.etUpdateNotificationInterval.addTextChangedListener {
            viewModel.updateNotificationIntervalStateField.toggleFieldState(it.toString().toLongNotNull())
        }
        viewModel.updateNotificationIntervalStateField.observeFrom(
            binding.etUpdateNotificationInterval,
            viewLifecycleOwner
        ) {
            binding.etUpdateNotificationInterval.isEnabled = it.isEnabled
            it.value.toString()
        }
        viewModel.updateNotificationIntervalStateField.bindHintError(
            viewLifecycleOwner,
            binding.tilUpdateNotificationInterval
        )

        binding.etStartPageUrl.bindToTextNotNull(viewModel.startPageUrlField)
        viewModel.startPageUrlField.observeFromText(binding.etStartPageUrl, viewLifecycleOwner)
        viewModel.startPageUrlField.bindHintError(viewLifecycleOwner, binding.tilStartPageUrl)

        viewModel.hasChanges.observe {
            refreshSaveMenuItem(it)
        }
    }

    override fun canNavigate(navigationAction: () -> Unit): Boolean {
        return !viewModel.navigateWithAlert(errorFieldFunc, navigationAction)
    }

    override fun onUpPressed(): Boolean {
        return if (!viewModel.navigateBackWithAlert(errorFieldFunc)) {
            super.onUpPressed()
        } else {
            true
        }
    }

    override fun onBackPressed(): Boolean {
        return if (!viewModel.navigateBackWithAlert(errorFieldFunc)) {
            super.onBackPressed()
        } else {
            true
        }
    }

    private fun refreshSaveMenuItem(isEnabled: Boolean) {
        saveMenuItem?.isEnabled = isEnabled
    }
}