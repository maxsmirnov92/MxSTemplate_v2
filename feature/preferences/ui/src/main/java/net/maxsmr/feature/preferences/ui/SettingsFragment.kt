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
import com.google.android.material.textfield.TextInputLayout
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
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

open class SettingsFragment : BaseNavigationFragment<SettingsViewModel>() {

    override val layoutId: Int = R.layout.fragment_settings

    override val viewModel: SettingsViewModel by viewModels()

    override val menuResId: Int = R.menu.menu_settings

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    protected val binding by viewBinding(FragmentSettingsBinding::bind)

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
            saveChanges()
            true
        } else {
            super.onMenuItemSelected(menuItem)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: SettingsViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)

        viewModel.maxDownloadsField.observeIntWithBind(binding.tilMaxDownloads)

        viewModel.connectTimeoutField.observeLongWithBind(binding.tilConnectTimeout)

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

        viewModel.startPageUrlField.observeTextWithBind(binding.tilStartPageUrl)

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

    protected open fun saveChanges() {
        viewModel.saveChanges(errorFieldFunc)
    }

    private fun refreshSaveMenuItem(isEnabled: Boolean) {
        saveMenuItem?.isEnabled = isEnabled
    }

    private fun Field<String>.observeTextWithBind(til: TextInputLayout) {
        val editText = til.editText ?: return
        editText.bindToTextNotNull(this)
        observeFromText(editText, viewLifecycleOwner)
        bindHintError(viewLifecycleOwner, til)
    }

    private fun Field<Long>.observeLongWithBind(til: TextInputLayout) {
        observeWithBind(til,
            { it.toLongNotNull() },
            { it.toString() })
    }

    private fun Field<Int>.observeIntWithBind(til: TextInputLayout) {
        observeWithBind(til,
            { it.toIntNotNull() },
            { it.toString() })
    }

    private fun <D> Field<D>.observeWithBind(
        til: TextInputLayout,
        toFieldValue: ((CharSequence?) -> D),
        formatFunc: (D) -> CharSequence?,
    ) {
        val editText = til.editText ?: return
        editText.bindTo(this, toFieldValue)
        observeFrom(editText, viewLifecycleOwner, formatFunc = formatFunc)
        bindHintError(viewLifecycleOwner, til)
    }

}