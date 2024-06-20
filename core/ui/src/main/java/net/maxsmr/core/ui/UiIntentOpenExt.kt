package net.maxsmr.core.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import net.maxsmr.commonutils.SendAction
import net.maxsmr.commonutils.openDocument
import net.maxsmr.commonutils.openEmailIntent
import net.maxsmr.commonutils.openSendDataIntent
import net.maxsmr.commonutils.openSystemBrowser

@JvmOverloads
fun Context.openEmailIntentWithToastError(
    email: String?,
    sendAction: SendAction = SendAction.SENDTO,
    sendIntentFunc: ((Intent) -> Unit)? = null,
    chooserIntentFunc: ((Intent) -> Unit)? = null,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
): Boolean {
    return openEmailIntent(
        email,
        sendAction,
        getString(R.string.chooser_title_send_email),
        sendIntentFunc,
        chooserIntentFunc,
        flags,
        options
    ) {
        Toast.makeText(this, R.string.error_intent_email, Toast.LENGTH_SHORT).show()
    }
}

/**
 * @param uri со схемой [URL_SCHEME_MAIL]
 * @param sendIntentFunc дополнительно можно указать subject, text и т.д.
 * @param chooserIntentFunc настройка chooser [Intent] при указании [chooserTitle]
 */
@JvmOverloads
fun Context.openEmailIntentWithToastError(
    uri: Uri,
    sendAction: SendAction = SendAction.SENDTO,
    sendIntentFunc: ((Intent) -> Unit)? = null,
    chooserIntentFunc: ((Intent) -> Unit)? = null,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
): Boolean {
    return openEmailIntent(
        uri,
        sendAction,
        getString(R.string.chooser_title_send_email),
        sendIntentFunc,
        chooserIntentFunc,
        flags,
        options
    ) {
        Toast.makeText(this, R.string.error_intent_email, Toast.LENGTH_SHORT).show()
    }
}

@JvmOverloads
fun Context.openSendDataIntentWithToastError(
    sendAction: SendAction = SendAction.SEND,
    sendIntentFunc: (Intent) -> Unit,
    chooserIntentFunc: ((Intent) -> Unit)? = null,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
): Boolean {
    return openSendDataIntent(sendAction, getString(R.string.chooser_title_send), sendIntentFunc, chooserIntentFunc, flags, options) {
        Toast.makeText(this, getString(R.string.error_intent_send), Toast.LENGTH_SHORT).show()
    }
}

fun Context.openDocumentWithToastError(
    type: String?,
    mimeTypes: List<String>,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
): Boolean {
    return openDocument(
        type,
        mimeTypes,
        flags,
        options,
        errorHandler = {
            Toast.makeText(this, R.string.error_intent_open_document, Toast.LENGTH_SHORT).show()
        })
}

@JvmOverloads
fun Context.openSystemBrowserWithToastError(
    uri: String,
    mimeType: String? = null,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
): Boolean = openSystemBrowser(uri, mimeType, flags, options) {
    Toast.makeText(this, R.string.error_intent_open_url, Toast.LENGTH_SHORT).show()
}