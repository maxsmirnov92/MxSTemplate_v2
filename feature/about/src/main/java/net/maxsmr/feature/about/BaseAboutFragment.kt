package net.maxsmr.feature.about

import android.view.MenuItem
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment

abstract class BaseAboutFragment<VM: BaseAboutViewModel>: BaseNavigationFragment<VM>() {

    override val layoutId: Int = R.layout.fragment_about

    override val menuResId: Int = R.menu.menu_about

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return  if (menuItem.itemId == R.id.actionRateApp) {
            viewModel.onRateAppSelected()
            true
        } else {
            super.onMenuItemSelected(menuItem)
        }
    }
}