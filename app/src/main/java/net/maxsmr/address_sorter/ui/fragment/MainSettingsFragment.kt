package net.maxsmr.address_sorter.ui.fragment

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.feature.preferences.ui.BaseSettingsFragment
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class MainSettingsFragment: BaseSettingsFragment() {

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper
}