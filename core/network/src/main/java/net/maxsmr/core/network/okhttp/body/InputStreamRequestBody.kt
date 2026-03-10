package net.maxsmr.core.network.okhttp.body

import android.content.ContentResolver
import android.net.Uri
import net.maxsmr.commonutils.media.length
import net.maxsmr.commonutils.media.mimeType
import net.maxsmr.commonutils.media.openInputStreamOrThrow
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

class InputStreamRequestBody(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val type: String? = null,
) : RequestBody() {

    override fun contentType(): MediaType? {
        val contentType = type ?: uri.mimeType(contentResolver)
        return contentType.toMediaTypeOrNull()
    }

    override fun contentLength(): Long {
        return uri.length(contentResolver) ?: -1
    }

    override fun writeTo(sink: BufferedSink) {
        uri.openInputStreamOrThrow(contentResolver).use { sink.writeAll(it.source()) }
    }
}