package net.maxsmr.core.network

import android.text.TextUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import net.maxsmr.commonutils.IStreamNotifier
import net.maxsmr.commonutils.REG_EX_FILE_NAME
import net.maxsmr.commonutils.copyStreamOrThrow
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.ProgressListener
import net.maxsmr.core.network.exceptions.HttpProtocolException.Companion.toHttpProtocolException
import net.maxsmr.core.utils.charsetForNameOrNull
import okhttp3.*
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ForwardingSink
import okio.ForwardingSource
import okio.Sink
import okio.Source
import okio.buffer
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("OkHttpExt")

private const val HEADER_CONTENT_TYPE = "Content-Type"
private const val HEADER_CONTENT_DISPOSITION = "Content-Disposition"
private const val ATTACHMENT_FILENAME = "filename"

fun OkHttpClient.executeCall(
    requestConfigurator: ((Request.Builder) -> Any?),
): Response? = try {
    executeCallOrThrow(requestConfigurator)
} catch (e: Exception) {
    logger.e("Execute call", e)
    null
}

@Throws(Exception::class)
fun OkHttpClient.executeCallOrThrow(
    requestConfigurator: ((Request.Builder) -> Any?),
): Response {
    val request = Request.Builder()
    requestConfigurator.invoke(request)
    return newCall(request.build()).execute()
}

suspend fun OkHttpClient.newCallSuspended(request: Request, checkSuccess: Boolean = true): Response =
    suspendCancellableCoroutine { continuation ->
        val call = newCall(request)
        continuation.invokeOnCancellation {
            call.cancel()
        }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(RuntimeException("Request failed", e))
            }

            override fun onResponse(call: Call, response: Response) {
                if (!checkSuccess || response.isSuccessful) {
                    continuation.resume(response)
                } else {
                    continuation.resumeWithException(response.toHttpProtocolException())
                }
            }
        })
    }

// region: Request

fun Request?.path(): String? =
    this?.let { TextUtils.join("/", it.url.pathSegments) }

fun Request?.asString(charset: Charset = Charset.defaultCharset()): String? {
    return try {
        asStringOrThrow(charset)
    } catch (e: IOException) {
        logger.e("Read request body to String", e)
        null
    }
}

@JvmOverloads
@Throws(IOException::class)
fun Request?.asStringOrThrow(charset: Charset = Charset.defaultCharset()): String? {
    this ?: return null
    val copy = newBuilder().build()
    val buffer = Buffer()
    copy.body?.writeTo(buffer)
    return buffer.readString(charset)
}

// endregion

// region Response: READ

fun Response?.asByteArray(previousDownloadedSize: Long? = null): ByteArray? = try {
    asByteArrayOrThrow(previousDownloadedSize)
} catch (e: IOException) {
    logger.e("Read response body to ByteArray", e)
    null
}

@Throws(IOException::class)
fun Response?.asByteArrayOrThrow(previousDownloadedSize: Long? = null): ByteArray? {
    skipBytesIfSupportedOrThrow(previousDownloadedSize)
    return this?.body?.bytes()
}

fun Response?.asString(previousDownloadedSize: Long? = null): String? = try {
    asStringOrThrow(previousDownloadedSize)
} catch (e: IOException) {
    logger.e("Read response body to String", e)
    null
}

@Throws(IOException::class)
fun Response?.asStringOrThrow(previousDownloadedSize: Long? = null): String? {
    skipBytesIfSupportedOrThrow(previousDownloadedSize)
    return this?.body?.string()
}

fun Response?.toOutputStream(
    outputStream: OutputStream,
    previousDownloadedSize: Long? = null,
    notifier: IStreamNotifier? = null,
): ResponseBody? = try {
    toOutputStreamOrThrow(outputStream, previousDownloadedSize, notifier)
} catch (e: IOException) {
    logger.e("Read response body to OutputStream", e)
    null
}

@Throws(IOException::class)
fun Response?.toOutputStreamOrThrow(
    outputStream: OutputStream,
    previousDownloadedSize: Long? = null,
    notifier: IStreamNotifier? = null,
): ResponseBody {
    val responseBody = this?.body ?: throw RuntimeException("Response body is missing")
    skipBytesIfSupportedOrThrow(previousDownloadedSize)
    responseBody.byteStream().copyToOutputStreamOrThrow(outputStream, notifier, responseBody.contentLength())
    return responseBody
}

// endregion

// region Response: COPY

/**
 * Вычитывает тело запроса в массив байт, не изменяя исходный [InputStream]
 */
fun Response?.asByteArrayCopy(): ByteArray? = try {
    asByteArrayCopyOrThrow()
} catch (e: IOException) {
    logger.e("Copy response body to ByteArray", e)
    null
}

@Throws(IOException::class)
fun Response?.asByteArrayCopyOrThrow(): ByteArray? {
    this ?: return null
    return cloneBufferOrThrow()?.use { it.readByteArray() }
}

/**
 * Вычитывает тело запроса в строку, не изменяя исходный [InputStream]
 */
fun Response?.asStringCopy(): Pair<String, Charset>? = try {
    asStringCopyOrThrow()
} catch (e: IOException) {
    logger.e("Copy response body to String", e)
    null
}

@Throws(IOException::class)
fun Response?.asStringCopyOrThrow(): Pair<String, Charset>? {
    this ?: return null
    val charset = header("Content-Encoding").charsetForNameOrNull() ?: Charset.defaultCharset()
    return cloneBufferOrThrow()?.use {
        Pair(it.readString(charset), charset)
    }
}

/**
 * Вычитывает тело запроса в [OutputStream], не изменяя исходный [InputStream]
 */
fun Response?.toOutputStreamCopy(
    outputStream: OutputStream?,
    notifier: IStreamNotifier? = null,
): ResponseBody? = try {
    toOutputStreamCopyOrThrow(outputStream, notifier)
} catch (e: IOException) {
    logger.e("Copy response body to OutputStream", e)
    null
}

@Throws(IOException::class)
fun Response?.toOutputStreamCopyOrThrow(
    outputStream: OutputStream?,
    notifier: IStreamNotifier? = null,
): ResponseBody? {
    outputStream ?: return null
    val responseBody = this?.body ?: return null
    val buffer = cloneBufferOrThrow() ?: return null
    buffer.inputStream().copyToOutputStreamOrThrow(outputStream, notifier, responseBody.contentLength())
    return responseBody
}

@Throws(IOException::class)
private fun Response.cloneBufferOrThrow(): Buffer? {
    val source: BufferedSource = body?.source() ?: return null
    // request the entire body.
    source.request(Long.MAX_VALUE)
    // clone buffer before reading from it
    return source.buffer.clone()
}

// endregion

fun isResponseOk(responseCode: Int): Boolean = responseCode in 200..299

fun Response.getContentTypeHeader() = header(HEADER_CONTENT_TYPE)

fun Response.getContentDispositionHeader() = header(HEADER_CONTENT_DISPOSITION)

fun Response.hasContentDisposition(type: ContentDispositionType): Boolean {
    return getContentDispositionHeader().hasContentDisposition(type)
}

/**
 * Варианты:
 * Content-Disposition: inline
 * Content-Disposition: attachment
 * Content-Disposition: attachment; filename="filename.jpg"
 * Content-Disposition: attachment; filename*=UTF-8''CV%20example.docx
 */
fun Response.getFileNameFromAttachmentHeader(): String {
    val disposition = getContentDispositionHeader()
    if (disposition == null || !disposition.hasContentDisposition(ContentDispositionType.ATTACHMENT)) {
        return EMPTY_STRING
    }
    val encodedName: String = if (disposition.contains("$ATTACHMENT_FILENAME*")) {
        disposition.substringAfter("''")
    } else if (disposition.contains(ATTACHMENT_FILENAME)) {
        disposition.substringAfter("$ATTACHMENT_FILENAME=").trimEnd { it == '"' }
    } else {
        EMPTY_STRING
    }
    return java.net.URLDecoder.decode(
        encodedName, Charset.defaultCharset().toString()
    )
        .takeIf { REG_EX_FILE_NAME.toRegex().matches(it) }
        .orEmpty()
}

private fun String?.hasContentDisposition(type: ContentDispositionType): Boolean {
    return this?.startsWith(type.value) == true
}

fun Headers?.toMap(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    if (this != null) {
        for (i in 0 until size) {
            val name = name(i)
            result[name] = this[name] ?: ""
        }
    }
    return result
}

fun Response.isResumeDownloadSupported(): Boolean {
    val acceptHeader = this.header("Accept-Ranges") ?: ""
    return acceptHeader.isNotEmpty() && !acceptHeader.equals("none", ignoreCase = true)
}

@Throws(IOException::class)
private fun Response?.skipBytesIfSupportedOrThrow(downloadedSize: Long?) {
    this ?: return
    if (downloadedSize != null && downloadedSize > 0) {
        if (isResumeDownloadSupported()) {
            body?.source()?.skip(downloadedSize)
        }
    }
}

@Throws(IOException::class)
private fun InputStream.copyToOutputStreamOrThrow(
    outputStream: OutputStream,
    notifier: IStreamNotifier?,
    contentLength: Long,
) {
    copyStreamOrThrow(outputStream,
        if (notifier != null) {
            object : IStreamNotifier {

                override val notifyInterval: Long = notifier.notifyInterval

                override fun onProcessing(
                    inputStream: InputStream,
                    outputStream: OutputStream,
                    bytesWrite: Long,
                    bytesTotal: Long,
                ): Boolean {
                    return notifier.onProcessing(
                        inputStream,
                        outputStream,
                        bytesWrite,
                        if (contentLength != -1L && contentLength > bytesWrite) {
                            contentLength
                        } else {
                            bytesTotal
                        }
                    )
                }
            }
        } else {
            null
        })
}

// TODO to domain
enum class Method(val value: String) {

    POST("POST"),
    GET("GET")
}

enum class ContentDispositionType(val value: String) {

    INLINE("inline"),
    ATTACHMENT("attachment"),
    FORM_DATA("form-data")
}

class ProgressRequestBody(
    private val delegateBody: RequestBody,
    private val listener: ProgressListener,
) : RequestBody() {

    private val contentLength = delegateBody.contentLength()

    private var writeCount = 0

    override fun contentType(): MediaType? {
        return delegateBody.contentType()
    }

    override fun contentLength(): Long {
        return try {
            delegateBody.contentLength()
        } catch (e: IOException) {
            -1L
        }
    }

    override fun writeTo(sink: BufferedSink) {
        writeCount++
        if (writeCount == 1) {
            delegateBody.writeTo(sink)
        } else {
            val bufferedSink = ProgressForwardingSink(sink).buffer()
            delegateBody.writeTo(bufferedSink)
            bufferedSink.flush()
        }
    }

    private inner class ProgressForwardingSink(delegate: Sink) : ForwardingSink(delegate) {

        private val startTime = System.currentTimeMillis()

        private var totalBytesWritten: Long = 0

        @Throws(IOException::class)
        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            totalBytesWritten += byteCount
            listener.notify(
                totalBytesWritten,
                contentLength,
                if (contentLength >= 0) {
                    totalBytesWritten >= contentLength
                } else {
                    false
                },
                startTime
            )
        }
    }
}

class ProgressResponseBody(
    private val delegateBody: ResponseBody,
    private val listener: ProgressListener,
) : ResponseBody() {

    private val contentLength = delegateBody.contentLength()

    private var bufferedSource: BufferedSource? = null

    override fun contentType(): MediaType? {
        return delegateBody.contentType()
    }

    override fun contentLength(): Long {
        return delegateBody.contentLength()
    }

    override fun source(): BufferedSource {
        return bufferedSource ?: ProgressForwardingSource(delegateBody.source()).buffer().apply {
            bufferedSource = this
        }
    }

    private inner class ProgressForwardingSource(source: Source) : ForwardingSource(source) {

        private val startTime = System.currentTimeMillis()

        private var totalBytesRead = 0L

        @Throws(IOException::class)
        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytesRead = super.read(sink, byteCount)
            // read() returns the number of bytes read, or -1 if this source is exhausted.
            totalBytesRead += if (bytesRead > 0) bytesRead else 0
            listener.notify(totalBytesRead, contentLength, bytesRead == -1L, startTime)
            return bytesRead
        }
    }
}