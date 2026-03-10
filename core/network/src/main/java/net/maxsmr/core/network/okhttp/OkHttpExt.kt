package net.maxsmr.core.network.okhttp

import android.content.ContentResolver
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import net.maxsmr.commonutils.REG_EX_FILE_NAME
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.media.name
import net.maxsmr.commonutils.model.toJSONObject
import net.maxsmr.commonutils.stream.StreamNotifier
import net.maxsmr.commonutils.stream.copyStreamOrThrow
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.text.charsetForNameOrNull
import net.maxsmr.core.ProgressListener
import net.maxsmr.core.network.okhttp.body.InputStreamRequestBody
import net.maxsmr.core.network.exceptions.HttpProtocolException
import net.maxsmr.core.network.exceptions.OkHttpException.Companion.orNetworkCause
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex
import okio.Options
import okio.buffer
import okio.sink
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.text.Charsets.UTF_16BE
import kotlin.text.Charsets.UTF_16LE
import kotlin.text.Charsets.UTF_32BE
import kotlin.text.Charsets.UTF_32LE
import kotlin.text.Charsets.UTF_8

private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("OkHttpExt")

const val HEADER_CONTENT_TYPE = "Content-Type"
private const val HEADER_CONTENT_ENCODING = "Content-Encoding"
private const val HEADER_CONTENT_DISPOSITION = "Content-Disposition"
private const val HEADER_ACCEPT_RANGES = "Accept-Ranges"
private const val ATTACHMENT_FILENAME = "filename"

private val UNICODE_BOMS =
    Options.of(
        // UTF-8.
        "efbbbf".decodeHex(),
        // UTF-16BE.
        "feff".decodeHex(),
        // UTF-32LE.
        "fffe0000".decodeHex(),
        // UTF-16LE.
        "fffe".decodeHex(),
        // UTF-32BE.
        "0000feff".decodeHex(),
    )

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
                continuation.resumeWithException(e.orNetworkCause())
            }

            override fun onResponse(call: Call, response: Response) {
                if (!checkSuccess || response.isSuccessful) {
                    continuation.resume(response)
                } else {
                    continuation.resumeWithException(HttpProtocolException(response))
                }
            }
        })
    }

// region: Request

fun Request.path(): String? =
    TextUtils.join("/", url.pathSegments)

fun Request.asString(charset: Charset = Charset.defaultCharset()): String? {
    return try {
        asStringOrThrow(charset)
    } catch (e: IOException) {
        logger.e("Read request body as String", e)
        null
    }
}

@JvmOverloads
@Throws(IOException::class)
fun Request.asStringOrThrow(charset: Charset = Charset.defaultCharset()): String {
    val copy = newBuilder().build()
    val buffer = Buffer()
    copy.body?.writeTo(buffer)
    return buffer.readString(charset)
}

@JvmOverloads
fun Request.appendValues(
    appendQueryParametersFunc: (HttpUrl.Builder.() -> Unit)? = null,
    appendHeadersFunc: (Request.Builder.() -> Unit)? = null,
    appendJsonFunc: (JSONObject.() -> Unit)? = null,
): Request {
    if (appendQueryParametersFunc == null && appendHeadersFunc == null && appendJsonFunc == null) {
        return this
    }

    var request = this

    appendQueryParametersFunc?.let {
        val url = request.url.newBuilder()
        appendQueryParametersFunc(url)
        request = request.newBuilder().url(url.build()).build()
    }

    body?.let { requestBody ->

        val contentType = requestBody.contentType()
        val subtype = contentType?.subtype
        val charset = contentType?.charset() ?: Charset.defaultCharset()

        val json: JSONObject? = appendJsonFunc?.let {
            if (subtype == null || subtype.contains("json", true)) {
                // если уже записан не json - не дописывать
                (request
                    .asString(charset)
                    ?.toJSONObject() ?: JSONObject()).also {
                    appendJsonFunc(it)
                }
            } else {
                null
            }
        }

        request = with(request.newBuilder()) {
            appendHeadersFunc?.invoke(this)
            if (json != null && (subtype.isNullOrEmpty() || !subtype.contains("json"))) {
                // json был дописан, но тип не тот
                removeHeader(HEADER_CONTENT_TYPE)
                addHeader(HEADER_CONTENT_TYPE, "application/json; charset=${charset.name()}")
            }
            build()
        }

        json?.let {
            request = request.newBuilder().tag(JSONObject::class.java, json)
                .method(
                    request.method,
                    json.toString().toRequestBody(request.body?.contentType())
                ).build()
        }
    }

    return request
}

fun HttpUrl.toQueryMap(): Map<String, String> {
    return this.queryParameterNames.associateWith { name ->
        this.queryParameter(name).orEmpty()
    }
}

// endregion

// region Response: READ

fun Response.asByteArray(previousDownloadedSize: Long? = null): ByteArray? = try {
    asByteArrayOrThrow(previousDownloadedSize)
} catch (e: IOException) {
    logger.e("Read response body as ByteArray", e)
    null
}

@Throws(IOException::class)
fun Response.asByteArrayOrThrow(previousDownloadedSize: Long? = null): ByteArray {
    skipBytesIfSupportedOrThrow(previousDownloadedSize)
    return this.body?.bytes() ?: ByteArray(0)
}

fun Response.asString(previousDownloadedSize: Long? = null): String? = try {
    asStringOrThrow(previousDownloadedSize)
} catch (e: IOException) {
    logger.e("Read response body as String", e)
    null
}

@Throws(IOException::class)
fun Response.asStringOrThrow(previousDownloadedSize: Long? = null): String {
    skipBytesIfSupportedOrThrow(previousDownloadedSize)
    return this.body?.string().orEmpty()
}

fun Response.write(
    outputStream: OutputStream,
    previousDownloadedSize: Long? = null,
    notifier: StreamNotifier? = null,
): ResponseBody? = try {
    writeOrThrow(outputStream, previousDownloadedSize, notifier)
} catch (e: IOException) {
    logger.e("Write response body to OutputStream", e)
    null
}

fun Response.write(
    outputStream: OutputStream,
    previousDownloadedSize: Long? = null,
    listener: ProgressListener? = null,
): ResponseBody? = try {
    writeOrThrow(outputStream, previousDownloadedSize, listener)
} catch (e: IOException) {
    logger.e("Write response body to OutputStream", e)
    null
}

@Throws(IOException::class)
fun Response.writeOrThrow(
    outputStream: OutputStream,
    previousDownloadedSize: Long? = null,
    listener: ProgressListener? = null,
): ResponseBody {
    return writeOrThrow(
        outputStream,
        previousDownloadedSize,
        listener?.let {
            ProgressListenerDelegate(it)
        }
    )
}

@Throws(IOException::class)
fun Response.writeOrThrow(
    outputStream: OutputStream,
    previousDownloadedSize: Long? = null,
    notifier: StreamNotifier? = null,
): ResponseBody {
    val responseBody = this.body
    skipBytesIfSupportedOrThrow(previousDownloadedSize)
    responseBody!!.byteStream().writeOrThrow(outputStream, notifier, responseBody.contentLength())
    return responseBody
}

@Throws(IOException::class)
private fun InputStream.writeOrThrow(
    outputStream: OutputStream,
    notifier: StreamNotifier?,
    contentLength: Long,
) {
    copyStreamOrThrow(
        outputStream,
        if (notifier != null) {
            StreamNotifierDelegate(notifier, contentLength)
        } else {
            null
        }
    )
}

fun Response.writeBuffered(
    outputStream: OutputStream,
    previousDownloadedSize: Long? = null,
): ResponseBody? = try {
    writeBufferedOrThrow(outputStream, previousDownloadedSize)
} catch (e: IOException) {
    logger.e("Buffered write response body to OutputStream", e)
    null
}

@Throws(IOException::class)
fun Response.writeBufferedOrThrow(
    outputStream: OutputStream,
    previousDownloadedSize: Long? = null,
): ResponseBody {
    val responseBody = this.body
    skipBytesIfSupportedOrThrow(previousDownloadedSize)
    val sink: BufferedSink = outputStream.sink().buffer()
    sink.writeAll(responseBody!!.source());
    sink.close()
    return responseBody
}

// endregion

// region Response: cloned

/**
 * Вычитывает тело запроса в массив байт, не изменяя исходный [InputStream]
 */
fun ResponseBody.asByteArrayCloned(): ByteArray? = try {
    asByteArrayClonedOrThrow()
} catch (e: IOException) {
    logger.e("Clone response body to ByteArray", e)
    null
}

@Throws(IOException::class)
fun ResponseBody.asByteArrayClonedOrThrow(): ByteArray {
    return source().cloneBufferOrThrow().use { it.readByteArray() }
}

/**
 * Вычитывает тело запроса в строку, не изменяя исходный [InputStream]
 */
fun ResponseBody.asStringCloned(): Pair<String, Charset>? =
    try {
        asStringClonedOrThrow()
    } catch (e: IOException) {
        Log.e("OkHttpExt", "Clone response body to String", e)
        null
    }

@Throws(IOException::class)
fun ResponseBody.asStringClonedOrThrow(): Pair<String, Charset> {
    val charset = getCharset()
    val source = source()
    return source.cloneBufferOrThrow().use {
        Pair(it.readString(source.readBomAsCharset(charset)), charset)
    }
}

/**
 * Вычитывает тело ответа в [OutputStream], не изменяя исходный [InputStream]
 */
fun Response.writeCloned(
    outputStream: OutputStream?,
    notifier: StreamNotifier? = null,
): ResponseBody? = try {
    writeClonedOrThrow(outputStream, notifier)
} catch (e: IOException) {
    logger.e("Clone response body to OutputStream", e)
    null
}

@Throws(IOException::class)
fun Response.writeClonedOrThrow(
    outputStream: OutputStream?,
    notifier: StreamNotifier? = null,
): ResponseBody? {
    outputStream ?: return null
    val responseBody = this.body
    val source = responseBody!!.source()
    val buffer = source.cloneBufferOrThrow()
    buffer.inputStream().writeOrThrow(outputStream, notifier, responseBody.contentLength())
    return responseBody
}

fun Response.toResponseBody(shouldClone: Boolean): ResponseBody {
    val body = body!!
    val bodyBytes = if (shouldClone) {
        body.asByteArrayClonedOrThrow()
    } else {
        asByteArrayOrThrow()
    }
    return bodyBytes.toResponseBody(body.contentType())
}

@Throws(IOException::class)
private fun BufferedSource.cloneBufferOrThrow(): Buffer {
    // request the entire body.
    this.request(Long.MAX_VALUE)
    // clone buffer before reading from it
    return this.buffer.clone()
}

internal fun BufferedSource.readBomAsCharset(default: Charset): Charset =
    when (select(UNICODE_BOMS)) {
        // a mapping from the index of encoding methods in UNICODE_BOMS to its corresponding encoding method
        0 -> UTF_8
        1 -> UTF_16BE
        2 -> UTF_32LE
        3 -> UTF_16LE
        4 -> UTF_32BE
        -1 -> default
        else -> throw AssertionError()
    }

// endregion

fun isResponseOk(responseCode: Int): Boolean = responseCode in 200..299

fun Response.getContentTypeHeader(): String {
    // или body?.contentType()?.type.orEmpty()
    return header(HEADER_CONTENT_TYPE)?.split(";")?.getOrNull(0).orEmpty()
}

fun Response.getCharset(): Charset {
    // сначала смотрим в Content-Encoding
    header(HEADER_CONTENT_ENCODING).charsetForNameOrNull().let {
        return if (it == null) {
            val defaultCharset = Charset.defaultCharset()
            // затем в Content-Type, где через ";" после имени типа
            body?.contentType()?.charset(defaultCharset) ?: defaultCharset
        } else {
            it
        }
    }
}

fun ResponseBody.getCharset(): Charset {
    val defaultCharset = Charset.defaultCharset()
    return contentType()?.charset(defaultCharset) ?: defaultCharset
}

fun Response.getContentDispositionHeader() = header(HEADER_CONTENT_DISPOSITION).orEmpty()

fun Response.getAcceptRangesHeader() = header(HEADER_ACCEPT_RANGES).orEmpty()

fun Response.hasContentDisposition(type: ContentDispositionType): Boolean {
    return getContentDispositionHeader().hasContentDisposition(type)
}

fun Response.hasBytesAcceptRanges(): Boolean {
    return getAcceptRangesHeader().equals("bytes", ignoreCase = true)
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
    if (!disposition.hasContentDisposition(ContentDispositionType.ATTACHMENT)) {
        return EMPTY_STRING
    }
    val encodedName: String = if (disposition.contains("$ATTACHMENT_FILENAME*")) {
        disposition.substringAfter("''")
    } else if (disposition.contains(ATTACHMENT_FILENAME)) {
        disposition.substringAfter("$ATTACHMENT_FILENAME=").trimEnd { it == '"' }
    } else {
        EMPTY_STRING
    }
    return URLDecoder.decode(
        encodedName,
        getCharset().name()
    ).takeIf { REG_EX_FILE_NAME.toRegex().matches(it) }.orEmpty()
}

private fun String?.hasContentDisposition(type: ContentDispositionType): Boolean {
    return this?.startsWith(type.value) == true
}

fun Headers.toPairs(): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    for (i in 0 until size) {
        val name = name(i)
        result.add(Pair(name, this[name].orEmpty()))
    }
    return result
}

fun Response.isResumeDownloadSupported(): Boolean {
    val acceptHeader = this.header("Accept-Ranges") ?: ""
    return acceptHeader.isNotEmpty() && !acceptHeader.equals("none", ignoreCase = true)
}

fun ContentResolver.getMultipartRequestBody(
    uris: Collection<Uri>,
    name: String,
): List<MultipartBody.Part> =
    uris.map { uri ->
        MultipartBody.Part.createFormData(
            name = name,
            filename = uri.name(this@getMultipartRequestBody),
            body = InputStreamRequestBody(
                contentResolver = this@getMultipartRequestBody,
                uri = uri
            )
        )
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

enum class ContentDispositionType(val value: String) {

    INLINE("inline"),
    ATTACHMENT("attachment"),
    FORM_DATA("form-data")
}

private class ProgressListenerDelegate(
    private val listener: ProgressListener,
) : StreamNotifier {

    override val notifyInterval: Long
        get() = listener.notifyInterval

    private var startTime: Long = 0

    override fun onProcessing(
        inputStream: InputStream,
        outputStream: OutputStream,
        bytesWrite: Long,
        bytesTotal: Long,
    ): Boolean {
        return listener.notify(
            bytesWrite,
            bytesTotal,
            startTime,
            System.currentTimeMillis(),
        )
    }

    override fun onCopyStart(bytesLeft: Long) {
        startTime = System.currentTimeMillis()
    }
}

private class StreamNotifierDelegate(
    private val notifier: StreamNotifier,
    private val contentLength: Long,
) : StreamNotifier {

    override val notifyInterval: Long = notifier.notifyInterval

    override fun onProcessing(
        inputStream: InputStream,
        outputStream: OutputStream,
        bytesWrite: Long,
        bytesTotal: Long,
    ): Boolean {
        return notifier.onProcessing(
            inputStream = inputStream,
            outputStream = outputStream,
            bytesWrite = bytesWrite,
            bytesTotal = contentLength
        )
    }
}