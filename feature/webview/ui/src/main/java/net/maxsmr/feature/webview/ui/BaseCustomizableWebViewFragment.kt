package net.maxsmr.feature.webview.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import net.maxsmr.commonutils.gui.bindToTextNotNull
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.commonutils.live.field.observeFromText
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.android.content.FileFormat
import net.maxsmr.core.network.exceptions.NetworkException
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.DialogRepresentation
import net.maxsmr.core.utils.charsetForNameOrNull
import net.maxsmr.feature.webview.data.client.BrowserInterceptWebViewClient
import net.maxsmr.feature.webview.data.client.InterceptWebViewClient
import net.maxsmr.feature.webview.data.client.InterceptWebViewClient.WebViewData
import net.maxsmr.feature.webview.ui.databinding.DialogInputUrlBinding
import net.maxsmr.feature.webview.ui.databinding.FragmentWebviewBinding
import net.maxsmr.permissionchecker.PermissionsHelper
import okhttp3.OkHttpClient
import java.nio.charset.Charset
import javax.inject.Inject

abstract class BaseCustomizableWebViewFragment<VM : BaseCustomizableWebViewModel> : BaseWebViewFragment<VM>() {

    abstract val okHttpClient: OkHttpClient?

    final override val layoutId: Int = R.layout.fragment_webview

    override val webView: WebView by lazy { binding.webView }

    override val swipeRefresh: SwipeRefreshLayout? by lazy { binding.swipeWebView }

    override val progress: ProgressBar by lazy { binding.pbToolbar }

    override val errorContainer: View by lazy { binding.errorContainer.root }

    override val menuResId: Int = R.menu.menu_web_view

    protected val binding by viewBinding(FragmentWebviewBinding::bind)

    protected val webViewCustomizer: WebViewCustomizer get() = viewModel.customizer

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    private var urlMenuItem: MenuItem? = null

    private var copyMenuItem: MenuItem? = null

    private var shareMenuItem: MenuItem? = null

    private var stopMenuItem: MenuItem? = null

    private var forwardMenuItem: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: VM) {
        super.onViewCreated(view, savedInstanceState, viewModel)
        with(binding) {
            webViewCustomizer.title.takeIf { it.isNotEmpty() }?.let {
                toolbar.title = it
            }
            toolbar.navigationIcon =
                ContextCompat.getDrawable(requireContext(), net.maxsmr.core.ui.R.drawable.ic_close_clear_cancel_white)
            errorContainer.btReload.setOnClickListener {
                doReloadWebView()
            }

        }
        viewModel.currentUrl.observe {
            refreshMenuItemsByCurrentUri(it)
        }
    }

    override fun handleAlerts(delegate: AlertFragmentDelegate<VM>) {
        super.handleAlerts(delegate)
        bindAlertDialog(BaseCustomizableWebViewModel.DIALOG_TAG_OPEN_URL) {
            val dialogBinding = DialogInputUrlBinding.inflate(LayoutInflater.from(requireContext()))

            dialogBinding.etUrl.bindToTextNotNull(viewModel.urlField)
            viewModel.urlField.observeFromText(dialogBinding.etUrl, viewLifecycleOwner)
            // бинд только хинта;
            // по текущему еррору (который здесь меняется при каждом инпуте)
            // еррор не выставляем - вместо этого дисейбл кнопки
            viewModel.urlField.hintLive.observe { hint ->
                dialogBinding.tilUrl.hint = hint?.get(requireContext())
            }

            dialogBinding.ibClear.setOnClickListener {
                viewModel.urlField.value = EMPTY_STRING
            }

            DialogRepresentation.Builder(requireContext(), it)
                .setCustomView(dialogBinding.root) {
                    viewModel.urlField.errorLive.observe { error ->
                        (this as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = error == null
                    }
                }
                .setPositiveButton(it.answers[0]) {
                    if (viewModel.onUrlConfirmed()) {
                        doInitReloadWebView()
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
        shareMenuItem = menu.findItem(R.id.action_share_link)
        refreshMenuItemsByCurrentUri(viewModel.currentUrl.value)

        stopMenuItem = menu.findItem(R.id.action_stop_loading)
        refreshStopMenuItem(viewModel.currentWebViewData.value)
        forwardMenuItem = menu.findItem(R.id.action_forward)
        refreshForwardMenuItem()
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_open_url -> {
                viewModel.onOpenUrlAction()
                true
            }

            R.id.action_copy_link -> {
                viewModel.onCopyLinkAction(requireContext())
                true
            }

            R.id.action_share_link -> {
                viewModel.onShareLinkAction(requireContext())
                true
            }

            R.id.action_stop_loading -> {
                webView.stopLoading()
                stopMenuItem?.isVisible = false
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

            R.id.action_clear_cache -> {
                webView.clearCache(true)
                true
            }

            R.id.action_clear_history -> {
                webView.clearHistory()
                refreshForwardMenuItem()
                true
            }

            else -> {
                super.onMenuItemSelected(menuItem)
            }
        }
    }

    override fun createWebViewClient(): InterceptWebViewClient =
        object : BrowserInterceptWebViewClient(requireContext(), okHttpClient) {

            override fun shouldOpenSystemBrowser(url: String): Boolean {
                val uri = Uri.parse(url)
                // переход во внешний браузер при наличии указанных названий параметров
                val hasNames = uri.queryParameterNames.any { name ->
                    webViewCustomizer.queryParameters.any { name == it }
                }
                return hasNames
            }
        }

    override fun doInitReloadWebView() {
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

//    override fun onFirstResourceLoading(hasData: Boolean, url: String?) {
//        super.onFirstResourceLoading(hasData, url)
//        binding.scrollWebView.isFillViewport = !hasData
//    }

//    override fun onFirstResourceSuccess(url: String) {
//        super.onFirstResourceSuccess(url)
//        binding.scrollWebView.isFillViewport = false
//    }

    override fun onResourceError(hasData: Boolean, url: Uri?, data: String?, exception: NetworkException?) {
        with(binding.errorContainer) {
            tvErrorUrl.setTextOrGone(url.toString())
            tvErrorDescription.setTextOrGone(exception?.message)
//            binding.scrollWebView.isFillViewport = true
        }
    }

    override fun onResourceChanged(resource: LoadState<WebViewData?>) {
        super.onResourceChanged(resource)
        refreshStopMenuItem(resource)
        refreshForwardMenuItem()
    }

    private fun refreshMenuItemsByCurrentUri(uri: Uri?) {
        val hasUri = uri != null
        urlMenuItem?.isVisible = hasUri
        copyMenuItem?.isVisible = hasUri
        shareMenuItem?.isVisible = hasUri
    }

    private fun refreshStopMenuItem(resource: LoadState<WebViewData?>?) {
        stopMenuItem?.isVisible = resource?.isLoading == true
    }

    private fun refreshForwardMenuItem() {
        forwardMenuItem?.isVisible = isWebViewInitialized && webView.canGoForward()
    }
}