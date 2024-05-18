package net.maxsmr.core.ui.components.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.MenuRes
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.AlertHandler

abstract class BaseMenuFragment<VM : BaseViewModel> : BaseVmFragment<VM>(), MenuProvider {

    @get:MenuRes
    protected open val menuResId: Int = 0

    protected var menu: Menu? = null

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: VM, alertHandler: AlertHandler) {
        val menuResId = menuResId
        if (menuResId != 0) {
            val menuHost: MenuHost = requireActivity()
            menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val menuHost: MenuHost = requireActivity()
        menuHost.removeMenuProvider(this)
        menu = null
    }

    @CallSuper
    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(menuResId, menu)
        this.menu = menu
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
}