package net.maxsmr.feature.download.ui

import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.base.alert.AlertHandler
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.permissionchecker.PermissionsHelper

@AndroidEntryPoint
class DownloadsStateFragment: BaseVmFragment<DownloadsParamsViewModel>() {

    override val layoutId: Int
        get() = TODO("Not yet implemented")
    override val viewModel: DownloadsParamsViewModel
        get() = TODO("Not yet implemented")
    override val permissionsHelper: PermissionsHelper
        get() = TODO("Not yet implemented")

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        viewModel: DownloadsParamsViewModel,
        alertHandler: AlertHandler,
    ) {
    }
}