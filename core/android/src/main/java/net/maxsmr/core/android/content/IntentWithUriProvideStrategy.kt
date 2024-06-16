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

    /**
     * @param isLocal является ли данная [uri] локальной
     */
    open class Data(
        val uri: Uri,
        val mimeType: String,
        private val isLocal: Boolean = true,
    ) {

        val flags: Int = if (isLocal) {
            Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
        } else {
            Intent.FLAG_ACTIVITY_NO_HISTORY
        }
    }
}

class ViewStrategy(override val data: IntentWithUriProvideStrategy.Data) : IntentWithUriProvideStrategy<IntentWithUriProvideStrategy.Data> {

    override fun intent(): Intent =
        getViewIntent().apply { setupIntent(this) }

    override fun setupIntent(intent: Intent) {
        with(this@ViewStrategy.data) {
            intent.setDataAndType(uri, mimeType)
            intent.flags = flags
        }
    }
}

class ShareStrategy(override val data: Data) : IntentWithUriProvideStrategy<ShareStrategy.Data> {

    override fun intent(): Intent =
        getSendIntent(SendAction.SEND).apply { setupIntent(this) }

    override fun setupIntent(intent: Intent) {
        with(this@ShareStrategy.data) {
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

    class Data(
        uri: Uri,
        mimeType: String,
        isLocal: Boolean = true,
        val description: String = EMPTY_STRING,
        val shortDescription: String = EMPTY_STRING,
        val emails: Collection<String> = emptyList(),
    ) : IntentWithUriProvideStrategy.Data(uri, mimeType, isLocal)
}