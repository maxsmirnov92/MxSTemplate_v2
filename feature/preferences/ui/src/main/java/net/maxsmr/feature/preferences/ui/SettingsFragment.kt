package net.maxsmr.feature.preferences.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import net.maxsmr.commonutils.conversion.toIntNotNull
import net.maxsmr.commonutils.conversion.toLongNotNull
import net.maxsmr.commonutils.gui.bindTo
import net.maxsmr.commonutils.gui.bindToTextNotNull
import net.maxsmr.commonutils.gui.scrollToView
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.live.field.observeFrom
import net.maxsmr.commonutils.live.field.observeFromText
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingApp
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.fields.bindHintError
import net.maxsmr.core.ui.fields.bindValue
import net.maxsmr.core.ui.fields.bindValueWithState
import net.maxsmr.core.ui.fields.setFieldValueIfEnabled
import net.maxsmr.feature.preferences.ui.databinding.FragmentSettingsBinding

abstract class BaseSettingsFragment : BaseNavigationFragment<SettingsViewModel>() {

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

        viewModel.loadByWiFiOnlyField.bindValue(viewLifecycleOwner, binding.switchLoadByWiFiOnly)
        viewModel.retryOnConnectionFailureField.bindValue(viewLifecycleOwner, binding.switchRetryOnConnectionFailure)
        viewModel.retryDownloadsField.bindValue(viewLifecycleOwner, binding.switchRetryDownloads)
        viewModel.disableNotificationsField.bindValue(viewLifecycleOwner, binding.switchDisableNotifications)

        binding.etUpdateNotificationInterval.addTextChangedListener {
            viewModel.updateNotificationIntervalStateField.setFieldValueIfEnabled(it.toString().toLongNotNull())
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

        viewModel.openLinksInExternalAppsField.bindValueWithState(viewLifecycleOwner, binding.switchOpenLinksInExternalApps, true)

        binding.etStartPageUrl.bindToTextNotNull(viewModel.startPageUrlField)
        viewModel.startPageUrlField.observeFromText(binding.etStartPageUrl, viewLifecycleOwner)
        viewModel.startPageUrlField.bindHintError(viewLifecycleOwner, binding.tilStartPageUrl)

        binding.spinnerRoutingApp.adapter = ArrayAdapter(
            requireContext(),
            net.maxsmr.core.ui.R.layout.item_spinner,
            net.maxsmr.core.ui.R.id.tvSpinner,
            resources.getStringArray(R.array.settings_field_routing_app_values)
        )
        viewModel.routingAppField.valueLive.observe {
            binding.spinnerRoutingApp.setSelection(RoutingApp.entries.indexOf(it))
        }
        binding.spinnerRoutingApp.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.routingAppField.value = RoutingApp.entries[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        viewModel.routingAppFromCurrentField.bindValue(viewLifecycleOwner, binding.switchRoutingAppFromCurrent)

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