package net.maxsmr.download.storage.file

import android.content.Intent
import android.net.Uri
import net.maxsmr.commonutils.SendAction
import net.maxsmr.commonutils.getSendIntent
import net.maxsmr.commonutils.getViewIntent


interface FileProvideStrategy<T : ViewFile.Data> {

    fun intent(data: T): Intent
}

object ViewFile : FileProvideStrategy<ViewFile.Data> {

    override fun intent(data: Data): Intent =
        getViewIntent().apply {
            setDataAndType(data.localUri, data.mimeType)
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

    open class Data(
        val localUri: Uri,
        val mimeType: String,
    )
}


object ShareFile : FileProvideStrategy<ShareFile.Data> {

    override fun intent(data: Data): Intent = getSendIntent(SendAction.SEND).apply {
        type = data.mimeType
        putExtra(Intent.EXTRA_STREAM, data.localUri)
        putExtra(Intent.EXTRA_SUBJECT, data.shortDescription)
        putExtra(Intent.EXTRA_TEXT, data.description)
        putExtra(Intent.EXTRA_EMAIL, data.emails.toTypedArray())
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    class Data(
        localUri: Uri,
        mimeType: String,
        val description: String,
        val shortDescription: String,
        val emails: Collection<String>,
    ) : ViewFile.Data(localUri, mimeType)
}