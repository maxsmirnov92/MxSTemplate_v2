package net.maxsmr.feature.download.ui.webview

import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebSettings
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import net.maxsmr.core.domain.entities.feature.download.DownloadParamsModel
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.webview.ui.BaseCustomizableWebViewFragment
import okhttp3.OkHttpClient

//@AndroidEntryPoint
class DownloadableWebViewFragment : BaseCustomizableWebViewFragment<DownloadableWebViewModel>() {

//    private val args by navArgs<DownloadableWebViewFragmentArgs>()

    override val viewModel by viewModels<DownloadableWebViewModel>()

    private val downloadsViewModel by activityViewModels<DownloadsViewModel>()

//    @Inject
//    @DownloaderOkHttpClient
    // при перехватах по некоторым урлам циклические редиректы
    override var okHttpClient: OkHttpClient? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: DownloadableWebViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)
        with(downloadsViewModel) {
            handleAlerts(requireContext(), AlertFragmentDelegate(this@DownloadableWebViewFragment, this))
            handleEvents(this@DownloadableWebViewFragment)
        }
    }

    override fun onSetupWebView(webSettings: WebSettings) {
        super.onSetupWebView(webSettings)

        webView.setDownloadListener(object : DownloadListener {
            override fun onDownloadStart(
                url: String?,
                userAgent: String?,
                contentDisposition: String?,
                mimetype: String?,
                contentLength: Long,
            ) {
                if (url.isNullOrEmpty()) return

                val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                val cookies = CookieManager.getInstance().getCookie(url)
                val headers = hashMapOf<String, String>()
                cookies?.takeIf { it.isNotEmpty() }?.let {
                    headers["cookie"] = cookies
                }
                userAgent?.takeIf { it.isNotEmpty() }?.let {
                    headers["User-Agent"] = it
                }
                downloadsViewModel.enqueueDownload(
                    DownloadParamsModel(
                        url,
                        fileName = fileName,
                        ignoreFileName = true,
                        headers = headers
                    )
                )
            }
        })
    }
}