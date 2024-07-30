package net.maxsmr.feature.webview.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import net.maxsmr.commonutils.copyToClipboard
import net.maxsmr.commonutils.getSendTextIntent
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.startActivitySafe
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.network.URL_SCHEME_HTTPS
import net.maxsmr.core.android.network.equalsIgnoreSubDomain
import net.maxsmr.core.android.network.isUrlValid
import net.maxsmr.core.android.network.toValidUri
import net.maxsmr.core.ui.fields.urlField

abstract class BaseCustomizableWebViewModel(
    state: SavedStateHandle,
) : BaseWebViewModel(state) {

    val urlField: Field<String> = state.urlField(
        hintResId = R.string.webview_alert_open_url_field_hint,
        withAsterisk = false,
        isRequired = true,
        isValidByBlank = true,
        schemeIfEmpty = URL_SCHEME_HTTPS
    )

    private val initialCustomizer = MutableLiveData<WebViewCustomizer?>(null)

    val hasInitialUrl: LiveData<Boolean> = initialCustomizer.map {
        // наличие домашней страницы == валидная исходная http/https-урла или about:blank
        it?.url.isUrlValid(orBlank = true)
    }

    abstract var customizer: WebViewCustomizer

    override fun onInitialized() {
        super.onInitialized()
        urlField.valueLive.observe {
            urlField.validateAndSetByRequired()
        }
        initialCustomizer.value = customizer
    }

    fun onUrlConfirmed(): Boolean {
        if (urlField.hasError) {
            return false
        }
        val newValue = urlField.value.toValidUri(orBlank = true, schemeIfEmpty = URL_SCHEME_HTTPS) ?: return false
        if (currentUrl.value?.host.equalsIgnoreSubDomain(newValue.host)) {
            return false
        }
        customizer = customizer.buildUpon().setUri(newValue).build()
        urlField.value = EMPTY_STRING
        return true
    }

    fun onOpenUrlAction() {
        currentUrl.value?.let {
            urlField.value = it.toString()
        }
        showOkDialog(DIALOG_TAG_OPEN_URL, TextMessage(R.string.webview_alert_open_url_title))
    }

    fun onOpenHomePageAction() {
        if (hasInitialUrl.value != true) return
        customizer = customizer.buildUpon().setUrl(initialCustomizer.value?.url).build()
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