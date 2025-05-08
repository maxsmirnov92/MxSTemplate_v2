package net.maxsmr.feature.webview.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.maxsmr.commonutils.copyToClipboard
import net.maxsmr.commonutils.flow.field.Field
import net.maxsmr.commonutils.getSendTextIntent
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.startActivitySafe
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.URL_SCHEME_HTTPS
import net.maxsmr.core.network.equalsIgnoreSubDomain
import net.maxsmr.core.network.isUrlValid
import net.maxsmr.core.network.toValidUri
import net.maxsmr.core.ui.field.urlField

abstract class BaseCustomizableWebViewModel(
    state: SavedStateHandle,
    context: Context
) : BaseWebViewModel(state, context) {

    val urlField: Field<String> = urlField(
        hintResId = R.string.webview_dialog_open_url_field_hint,
        withAsterisk = false,
        isRequired = true,
        isValidByBlank = true,
        schemeIfEmpty = URL_SCHEME_HTTPS
    )

    val hasInitialUrl: StateFlow<Boolean> by lazy {
        initialCustomizer.map {
            // наличие домашней страницы == валидная исходная http/https-урла или about:blank
            it?.url.isUrlValid(orBlank = true)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    }

    private val initialCustomizer = MutableStateFlow<WebViewCustomizer?>(null)

    abstract var customizer: WebViewCustomizer

    override fun onInitialized() {
        urlField.valueFlow.observe {
            urlField.validateAndSetByRequired()
        }
        initialCustomizer.value = customizer
    }

    fun onUrlConfirmed(): Boolean {
        if (urlField.hasError) {
            return false
        }
        val newValue = urlField.value.toValidUri(orBlank = true, schemeIfEmpty = URL_SCHEME_HTTPS) ?: return false
        if (currentWebViewData.value.isSuccess && currentUrl.value.equalsIgnoreSubDomain(newValue)) {
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
        showOkDialog(DIALOG_TAG_OPEN_URL, TextMessage(R.string.webview_dialog_open_url_title))
    }

    fun onOpenHomePageAction() {
        if (!hasInitialUrl.value) return
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