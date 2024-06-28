package net.maxsmr.feature.download.ui.webview

import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.live.zip
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.domain.entities.feature.download.DownloadParamsModel
import net.maxsmr.core.ui.fields.fileNameField
import net.maxsmr.core.ui.fields.subDirNameField
import net.maxsmr.feature.download.ui.R
import net.maxsmr.feature.webview.ui.BaseCustomizableWebViewModel

typealias ParamsModelWithType = Pair<DownloadParamsModel, String?>

abstract class BaseDownloadableWebViewModel(state: SavedStateHandle) : BaseCustomizableWebViewModel(state) {

    val fileNameField: Field<String> = state.fileNameField(isRequired = true)

    val subDirNameField: Field<String> = state.subDirNameField()

    val canStartDownload = zip(fileNameField.errorLive, subDirNameField.errorLive) { e1, e2 ->
        e1 == null && e2 == null
    }

    override fun onInitialized() {
        super.onInitialized()
        fileNameField.valueLive.observe {
            fileNameField.validateAndSetByRequired()
        }
        subDirNameField.valueLive.observe {
            subDirNameField.validateAndSetByRequired()
        }
    }

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
            // в любом случае будет непустое от пользователя,
            // перебивать из ответа не надо
            ignoreFileName = true,
            headers = headers
        )

        fileNameField.value = fileName

        AlertDialogBuilder(DIALOG_TAG_SAVE_AS)
            .setTitle(TextMessage(R.string.download_alert_save_as_title))
            .setAnswers(
                Alert.Answer(android.R.string.ok),
                Alert.Answer(android.R.string.cancel)
            )
            .setExtraData(ParamsModelWithType(model, mimetype))
            .build()
    }

    companion object {

        const val DIALOG_TAG_SAVE_AS = "save_as"
    }
}