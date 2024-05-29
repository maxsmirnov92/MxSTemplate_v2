package net.maxsmr.feature.webview.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.copyToClipboard
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.actions.ToastAction
import net.maxsmr.core.android.network.toUrlOrNull

abstract class BaseCustomizableWebViewModel(
    state: SavedStateHandle
) : BaseWebViewModel(state) {

    abstract var customizer: WebViewCustomizer

    fun onOpenUrlAction() {
        showOkDialog(DIALOG_TAG_OPEN_URL, TextMessage(R.string.webview_alert_open_url_message))
    }

    fun onUrlSelected(url: String): Boolean {
        return if (url.toUrlOrNull() != null) {
            customizer = customizer.buildUpon().setUrl(url).build()
            true
        } else {
            showToast(ToastAction(TextMessage(R.string.webview_toast_url_not_valid)))
            false
        }
    }

    fun onCopyLinkAction(context: Context) {
        currentWebViewData.value?.first?.data?.url?.takeIf { it.isNotEmpty() }?.let {
            copyToClipboard(context, "page link", it)
            showToast(ToastAction(TextMessage(net.maxsmr.core.android.R.string.toast_link_copied_to_clipboard_message)))
        }
    }

    companion object {

        const val DIALOG_TAG_OPEN_URL = "open_url"
    }
}