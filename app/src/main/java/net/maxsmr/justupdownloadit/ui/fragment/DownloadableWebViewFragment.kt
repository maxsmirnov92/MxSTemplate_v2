package net.maxsmr.justupdownloadit.ui.fragment

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.feature.download.ui.webview.BaseDownloadableWebViewFragment
import net.maxsmr.justupdownloadit.ui.DownloadableWebViewModel
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class DownloadableWebViewFragment: BaseDownloadableWebViewFragment<DownloadableWebViewModel>() {

//    private val args by navArgs<DownloadableWebViewFragmentArgs>()

    override val viewModel: DownloadableWebViewModel by viewModels()

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper
}