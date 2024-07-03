package net.maxsmr.mxstemplate.ui.fragment

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.feature.download.ui.webview.BaseDownloadableWebViewFragment
import net.maxsmr.mxstemplate.ui.BrowserWebViewModel
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class BrowserWebViewFragment : BaseDownloadableWebViewFragment<BrowserWebViewModel>() {

    override val viewModel: BrowserWebViewModel by viewModels()

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper
}