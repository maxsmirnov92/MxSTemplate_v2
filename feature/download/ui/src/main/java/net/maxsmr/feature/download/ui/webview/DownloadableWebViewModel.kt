package net.maxsmr.feature.download.ui.webview

import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.domain.entities.feature.download.DownloadParamsModel
import net.maxsmr.feature.download.ui.R
import net.maxsmr.feature.webview.ui.BaseCustomizableWebViewModel
import net.maxsmr.feature.webview.ui.WebViewCustomizer

class DownloadableWebViewModel(state: SavedStateHandle): BaseCustomizableWebViewModel(state) {

    override var customizer: WebViewCustomizer = DownloadableWebViewFragmentArgs.fromSavedStateHandle(state).customizer

    fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
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

        val model = DownloadParamsModel(
            url,
            fileName = fileName,
            ignoreFileName = true,
            headers = headers
        )

        AlertBuilder(DIALOG_TAG_SAVE_AS)
            .setTitle(TextMessage(R.string.download_alert_save_as_title))
            .setAnswers(
                Alert.Answer(android.R.string.ok),
                Alert.Answer(android.R.string.cancel))
            .setExtraData(model)
            .build()
    }

    companion object {

        const val DIALOG_TAG_SAVE_AS = "save_as"
    }
}