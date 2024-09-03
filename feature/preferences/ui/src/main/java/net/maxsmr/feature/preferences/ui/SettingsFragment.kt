package net.maxsmr.feature.preferences.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import net.maxsmr.commonutils.gui.scrollToView
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingApp
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.fields.bindValue
import net.maxsmr.feature.preferences.ui.databinding.FragmentSettingsBinding

abstract class BaseSettingsFragment : BaseNavigationFragment<SettingsViewModel>() {

    override val layoutId: Int = R.layout.fragment_settings

    override val viewModel: SettingsViewModel by viewModels()

    override val menuResId: Int = R.menu.menu_settings

    private val binding by viewBinding(FragmentSettingsBinding::bind)

    private val fieldViewsMap: Map<Field<*>, View> by lazy { emptyMap() }

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

        binding.spinnerRoutingApp.adapter = ArrayAdapter(
            requireContext(),
            net.maxsmr.core.ui.R.layout.item_spinner,
            net.maxsmr.core.ui.R.id.tvSpinner,
            resources.getStringArray(R.array.settings_field_routing_app_values)
        )
        viewModel.routingAppField.valueLive.observe {
            binding.spinnerRoutingApp.setSelection(RoutingApp.entries.indexOf(it))
        }
        binding.spinnerRoutingApp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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