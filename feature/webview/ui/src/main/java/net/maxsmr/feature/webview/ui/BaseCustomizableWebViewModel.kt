package net.maxsmr.feature.webview.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.copyToClipboard
import net.maxsmr.commonutils.getSendTextIntent
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.startActivitySafe
import net.maxsmr.core.android.base.actions.ToastAction
import net.maxsmr.core.android.network.SCHEME_HTTPS
import net.maxsmr.core.android.network.equalsIgnoreSubDomain
import net.maxsmr.core.ui.fields.urlField

abstract class BaseCustomizableWebViewModel(
    state: SavedStateHandle,
) : BaseWebViewModel(state) {

    abstract var customizer: WebViewCustomizer

    val urlField: Field<String> = state.urlField(
        initialValue = "$SCHEME_HTTPS://",
        hintResId = R.string.webview_alert_open_url_field_hint,
        withAsterisk = false,
        isRequired = true
    )

    override fun onInitialized() {
        super.onInitialized()
        urlField.valueLive.observe {
            urlField.validateAndSetByRequired()
        }
    }

    fun onUrlConfirmed(): Boolean {
        if (urlField.hasError) {
            return false
        }
        val newValue = urlField.value
        if (currentUrl.value.toString().equalsIgnoreSubDomain(newValue)) {
            return false
        }
        customizer = customizer.buildUpon().setUrl(newValue).build()
        urlField.value = "$SCHEME_HTTPS://"
        return true
    }

    fun onOpenUrlAction() {
        currentUrl.value?.let {
            urlField.value = it.toString()
        }
        showOkDialog(DIALOG_TAG_OPEN_URL, TextMessage(R.string.webview_alert_open_url_title))
    }

    fun onCopyLinkAction(context: Context) {
        currentUrl.value?.let {
            context.copyToClipboard(context.getString(R.string.webview_url_link_title), it.toString())
            showToast(TextMessage(net.maxsmr.core.ui.R.string.toast_link_copied_to_clipboard_message))
        }
    }

    fun onShareLinkAction(context: Context) {
        currentUrl.value?.let {
            context.startActivitySafe(getSendTextIntent(it.toString()).apply {
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.webview_url_link_title))
            }) {
                showToast(TextMessage(net.maxsmr.core.ui.R.string.error_intent_send))
            }
        }
    }

    companion object {

        const val DIALOG_TAG_OPEN_URL = "open_url"
    }
}