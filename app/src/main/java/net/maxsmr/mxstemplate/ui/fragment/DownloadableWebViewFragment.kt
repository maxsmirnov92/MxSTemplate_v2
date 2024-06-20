package net.maxsmr.mxstemplate.ui.fragment

import androidx.fragment.app.viewModels
import net.maxsmr.feature.download.ui.webview.BaseDownloadableWebViewFragment
import net.maxsmr.mxstemplate.ui.DownloadableWebViewModel

class DownloadableWebViewFragment(): BaseDownloadableWebViewFragment<DownloadableWebViewModel>() {

//    private val args by navArgs<DownloadableWebViewFragmentArgs>()

    override val viewModel: DownloadableWebViewModel by viewModels()
}