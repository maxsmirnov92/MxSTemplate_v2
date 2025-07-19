package net.maxsmr.core.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import net.maxsmr.commonutils.getSendEmailIntent
import net.maxsmr.commonutils.getSendIntent
import net.maxsmr.commonutils.openDocument
import net.maxsmr.commonutils.openViewUrl
import net.maxsmr.commonutils.startActivitySafe
import net.maxsmr.commonutils.wrapChooser

fun Context.openEmailIntentWithToastError(
    email: String?,
    isSendTo: Boolean = true,
    subject: String? = null,
    text: String?,
    addresses: List<String>? = null,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
): Boolean {
    val intent = getSendEmailIntent(
        email,
        isSendTo,
        subject,
        text,
        addresses
    )?.addFlags(flags) ?: return false
    return startActivitySafe(
        intent,
        errorHandler = {
            Toast.makeText(this, R.string.error_intent_email, Toast.LENGTH_SHORT).show()
        }
    )
}

@JvmOverloads
fun Context.openSendDataIntentWithToastError(
    isMultiple: Boolean = false,
    sendIntentFunc: Intent.() -> Unit,
): Boolean {
    return startActivitySafe(
        getSendIntent(isMultiple).apply { sendIntentFunc(this) },
        errorHandler = {
            Toast.makeText(this, getString(R.string.error_intent_send), Toast.LENGTH_SHORT).show()
        }
    )
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
fun Context.openViewUrlWithToastError(
    uri: String,
    mimeType: String? = null,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
): Boolean = openViewUrl(uri, mimeType, flags, options) {
    Toast.makeText(this, R.string.error_intent_open_url, Toast.LENGTH_SHORT).show()
}

fun Context.openAnyIntentWithToastError(
    intent: Intent,
    chooserTitle: String? = null,
    intentFunc: ((Intent) -> Unit)? = null,
    chooserIntentFunc: ((Intent) -> Unit)? = null,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
    @StringRes errorResId: Int = R.string.error_intent_any,
): Boolean {
    return startActivitySafe(
        intent.apply {
            intentFunc?.invoke(this)
            addFlags(flags)
        }.wrapChooser(chooserTitle).apply {
            chooserIntentFunc?.invoke(this)
        },
        options = options,
    ) {
        Toast.makeText(this, errorResId, Toast.LENGTH_SHORT).show()
    }
}