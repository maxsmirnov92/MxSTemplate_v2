package net.maxsmr.core.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.Toast
import net.maxsmr.commonutils.getOpenDocumentIntent
import net.maxsmr.commonutils.getViewUrlIntent
import net.maxsmr.commonutils.media.getMimeTypeFromUrl
import net.maxsmr.commonutils.startActivitySafe

@JvmOverloads
fun Context.openSystemBrowser(
    uri: Uri,
    mimeType: String? = getMimeTypeFromUrl(uri.toString())
): Boolean = startActivitySafe(getViewUrlIntent(uri, mimeType)) {
    Toast.makeText(this, net.maxsmr.core.android.R.string.error_intent_open_url, Toast.LENGTH_SHORT).show()
}

fun Activity.openDocument(
    type: String?,
    mimeTypes: List<String>,
    requestCode: Int?): Boolean =
    startActivitySafe(getOpenDocumentIntent(type, mimeTypes), requestCode) {
        Toast.makeText(this, net.maxsmr.core.android.R.string.error_intent_open_document, Toast.LENGTH_SHORT).show()
    }