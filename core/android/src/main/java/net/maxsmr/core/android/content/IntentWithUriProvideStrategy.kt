package net.maxsmr.core.android.content

import android.content.Intent
import android.net.Uri
import net.maxsmr.commonutils.SendAction
import net.maxsmr.commonutils.getSendIntent
import net.maxsmr.commonutils.getViewIntent
import net.maxsmr.commonutils.text.EMPTY_STRING

interface IntentWithUriProvideStrategy<T : IntentWithUriProvideStrategy.Data> {

    val data: T

    fun intent(): Intent

    fun setupIntent(intent: Intent)

    interface Data {

        val uri: Uri

        val mimeType: String

        val flags: Int
            // для перестраховки FLAG_GRANT_READ_URI_PERMISSION указываем всегда
            // даже для content:// схем в публичных папках, т.к. некоторые приложения явно того требуют
            get() = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }
}

class ViewIntentStrategy(override val data: ViewData) : IntentWithUriProvideStrategy<ViewIntentStrategy.ViewData> {

    override fun intent(): Intent =
        getViewIntent().apply { setupIntent(this) }

    override fun setupIntent(intent: Intent) {
        with(this@ViewIntentStrategy.data) {
            intent.setDataAndType(uri, mimeType)
            intent.flags = flags
        }
    }

    data class ViewData(
        override val uri: Uri,
        override val mimeType: String,
    ) : IntentWithUriProvideStrategy.Data
}

class ShareIntentStrategy(override val data: ShareData) : IntentWithUriProvideStrategy<ShareIntentStrategy.ShareData> {

    override fun intent(): Intent =
        getSendIntent(SendAction.SEND).apply { setupIntent(this) }

    override fun setupIntent(intent: Intent) {
        with(this@ShareIntentStrategy.data) {
            intent.type = mimeType
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.putExtra(Intent.EXTRA_SUBJECT, shortDescription)
            intent.putExtra(Intent.EXTRA_TEXT, description)
            if (emails.isNotEmpty()) {
                intent.putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
            }
            intent.flags = flags
        }
    }

    class ShareData(
        override val uri: Uri,
        override val mimeType: String,
        val description: String = EMPTY_STRING,
        val shortDescription: String = EMPTY_STRING,
        val emails: Collection<String> = emptyList(),
    ) : IntentWithUriProvideStrategy.Data
}