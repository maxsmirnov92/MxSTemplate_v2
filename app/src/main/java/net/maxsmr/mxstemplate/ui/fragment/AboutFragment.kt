package net.maxsmr.mxstemplate.ui.fragment

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.feature.about.BaseAboutFragment
import net.maxsmr.mxstemplate.ui.AboutViewModel
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class AboutFragment: BaseAboutFragment<AboutViewModel>() {

    override val viewModel: AboutViewModel by viewModels()

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

}