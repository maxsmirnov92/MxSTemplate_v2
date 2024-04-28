package net.maxsmr.download.model

import android.content.IntentSender
import android.net.Uri

data class IntentSenderParams(
    val downloadId: Long,
    val existingUri: Uri?,
    val resourceName: String,
    val intentSender: IntentSender
)