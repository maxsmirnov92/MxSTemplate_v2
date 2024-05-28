package net.maxsmr.feature.webview.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.EditText
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.commonutils.copyToClipboard
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.core.android.base.actions.ToastAction
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.android.content.FileFormat
import net.maxsmr.core.di.DownloaderOkHttpClient
import net.maxsmr.core.ui.alert.representation.DialogRepresentation
import net.maxsmr.core.utils.charsetForNameOrNull
import okhttp3.OkHttpClient
import net.maxsmr.feature.webview.data.client.BrowserInterceptWebViewClient
import net.maxsmr.feature.webview.data.client.InterceptWebViewClient
import net.maxsmr.feature.webview.data.client.InterceptWebViewClient.WebViewData
import net.maxsmr.feature.webview.ui.databinding.FragmentWebviewBinding
import net.maxsmr.permissionchecker.PermissionsHelper
import java.nio.charset.Charset
import javax.inject.Inject

@AndroidEntryPoint
class WebViewFragment : BaseWebViewFragment<WebViewModel>() {

    override val layoutId: Int = R.layout.fragment_webview

    override val webView: WebView by lazy { binding.containerWebView.webView }

    override val progress: ProgressBar by lazy { binding.pbToolbar }

    override val emptyErrorContainer: View by lazy { binding.containerWebView.errorContainer.root }

    override val viewModel by viewModels<WebViewModel>()

    override val menuResId: Int = R.menu.menu_web_view

//    private val args by navArgs<WebViewFragmentArgs>()

    private val webViewCustomizer: WebViewCustomizer get() = viewModel.customizer

    private val binding by viewBinding(FragmentWebviewBinding::bind)

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

//    @Inject
//    @DownloaderOkHttpClient
//    lateinit var okHttpClient: OkHttpClient

    private var urlMenuItem: MenuItem? = null

    private var copyMenuItem: MenuItem? = null

    private var forwardMenuItem: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: WebViewModel) {
        with(binding) {
            webViewCustomizer.title.takeIf { it.isNotEmpty() }?.let {
                toolbar.title = it
            }
            containerWebView.errorContainer.retryButton.setOnClickListener { doReloadWebView() }
        }
        super.onViewCreated(view, savedInstanceState, viewModel)

        bindAlertDialog(WebViewModel.DIALOG_TAG_OPEN_URL) {
            val customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_input_url, null)
            val urlEdit = customView.findViewById<EditText>(R.id.etUrl)
            DialogRepresentation.Builder(requireContext(), it)
//                .setThemeResId(com.google.android.material.R.style.Theme_MaterialComponents_DayNight)
                .setCustomView(customView)
                .setCancelable(true)
                .setPositiveButton(it.answers[0]) {
                    if (viewModel.onUrlSelected(urlEdit.text.toString().trim())) {
                        doReloadWebView()
                    }
                }
                .build()
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        urlMenuItem = menu.findItem(R.id.action_open_url).apply {
            isVisible = viewModel.customizer.canInputUrls
        }
        copyMenuItem = menu.findItem(R.id.action_copy_link)
        refreshCopyLinkMenuItem(viewModel.currentWebViewData.value?.first)
        forwardMenuItem = menu.findItem(R.id.action_forward)
        refreshForwardMenuItem()
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_open_url -> {
                viewModel.onOpenUrlAction()
                true
            }
            R.id.action_refresh -> {
                webView.reload()
                true
            }
            R.id.action_copy_link -> {
                viewModel.onCopyLinkAction(requireContext())
                true
            }
            R.id.action_forward -> {
                if (webView.canGoForward()) {
                    webView.goForward()
                    true
                } else {
                    false
                }
            }
            else -> {
                super.onMenuItemSelected(menuItem)
            }
        }
    }

    override fun createWebViewClient(): InterceptWebViewClient =
        object : BrowserInterceptWebViewClient(requireContext(), null) { // okHttpClient

            override fun shouldOpenSystemBrowser(url: String): Boolean {
                val uri = Uri.parse(url)
                // переход во внещний браузер при наличии указанных названий параметров
                val hasNames = uri.queryParameterNames.any { name ->
                    webViewCustomizer.queryParameters.any { name == it }
                }
                return hasNames
            }
        }

    override fun doReloadWebView() {
        val customizer = webViewCustomizer
        val url = customizer.url
        val data = customizer.data
        when {
            url.isNotEmpty() -> {
                loadUrl(url)
            }

            data != null -> {
                loadDataWithBaseUrl(
                    url,
                    data.data,
                    data.mimeType,
                    data.charset.charsetForNameOrNull() ?: Charset.defaultCharset(),
                    forceBase64 = data.forceBase64
                )
            }

            else -> {
                loadDataWithBaseUrl(
                    null,
                    requireContext().getString(net.maxsmr.core.android.R.string.no_data),
                    FileFormat.TEXT.mimeType
                )
            }
        }
    }

    override fun onFirstResourceChanged(resource: LoadState<WebViewData>) {
        super.onFirstResourceChanged(resource)
        with(binding.containerWebView) {
            if (resource.isLoading || resource.isSuccessWithData()) {
                errorContainer.retryButton.isVisible = false
            } else {
                // повтор целесообразен только при наличии исходной урлы
                errorContainer.retryButton.isVisible = webViewCustomizer.url.isNotEmpty()
            }
        }
    }

    override fun onResourceChanged(resource: LoadState<WebViewData>, isFirst: Boolean) {
        refreshCopyLinkMenuItem(resource)
        refreshForwardMenuItem()
    }

    private fun refreshCopyLinkMenuItem(resource: LoadState<WebViewData>?) {
        copyMenuItem?.isVisible = resource?.hasData { it?.url?.isNotEmpty() == true } == true
    }

    private fun refreshForwardMenuItem() {
        forwardMenuItem?.isVisible =  webView.canGoForward()
    }
}