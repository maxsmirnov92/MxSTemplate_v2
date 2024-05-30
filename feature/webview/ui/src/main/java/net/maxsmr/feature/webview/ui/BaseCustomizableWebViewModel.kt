package net.maxsmr.feature.webview.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.copyToClipboard
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.actions.ToastAction
import net.maxsmr.core.ui.fields.urlField

abstract class BaseCustomizableWebViewModel(
    state: SavedStateHandle,
) : BaseWebViewModel(state) {

    abstract var customizer: WebViewCustomizer

    lateinit var startUrl: String
        private set

    val urlField: Field<String> = state.urlField(
        hintResId = R.string.webview_alert_open_url_field_hint,
        withAsterisk = false,
        isRequired = true
    )

    override fun onInitialized() {
        super.onInitialized()
        startUrl = customizer.url
        urlField.valueLive.observe {
            urlField.validateAndSetByRequired()
        }
    }

    fun onOpenUrlAction() {
        showOkDialog(DIALOG_TAG_OPEN_URL, TextMessage(R.string.webview_alert_open_url_title))
    }

    fun onUrlConfirmed(): Boolean {
        if (urlField.hasError) {
            return false
        }
        customizer = customizer.buildUpon().setUrl(urlField.value).build()
        urlField.value = EMPTY_STRING
        return true
    }

    fun onCopyLinkAction(context: Context) {
        currentWebViewData.value?.first?.data?.url?.takeIf { it.isNotEmpty() }?.let {
            copyToClipboard(context, "page link", it)
            showToast(ToastAction(TextMessage(net.maxsmr.core.ui.R.string.toast_link_copied_to_clipboard_message)))
        }
    }

    companion object {

        const val DIALOG_TAG_OPEN_URL = "open_url"
    }
}