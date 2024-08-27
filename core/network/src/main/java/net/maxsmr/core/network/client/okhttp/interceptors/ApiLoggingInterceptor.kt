package net.maxsmr.core.network.client.okhttp.interceptors

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okhttp3.internal.platform.Platform
import okio.Buffer
import okio.GzipSource
import java.io.EOFException
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class ApiLoggingInterceptor @JvmOverloads constructor(
    private val logger: Logger = Logger.DEFAULT,
) : Interceptor {

    @Volatile
    private var headersToRedact = emptySet<String>()

    @set:JvmName("level")
    @Volatile
    var level = Level.BODY

    enum class Level {
        BODY,
        HEADERS_AND_BODY
    }

    fun interface Logger {

        fun log(message: String)

        companion object {

            /** A [Logger] defaults output appropriate for the current platform. */
            @JvmField
            val DEFAULT: Logger = DefaultLogger()

            private class DefaultLogger : Logger {

                override fun log(message: String) {
                    Platform.get().log(message)
                }
            }
        }
    }

    fun setLevel(level: Level) = apply {
        this.level = level
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val level = this.level

        val request = chain.request()

        val logHeaders = level == Level.HEADERS_AND_BODY

        val requestBody = request.body

        val connection = chain.connection()
        var requestStartMessage =
            ("--> ${request.method} ${request.url}${if (connection != null) " " + connection.protocol() else ""}")
        if (!logHeaders && requestBody != null) {
            requestStartMessage += " (${requestBody.contentLength()}-byte body)"
        }
        log(requestStartMessage)

        if (logHeaders) {
            val headers = request.headers

            if (requestBody != null) {
                // Request body headers are only present when installed as a network interceptor. When not
                // already present, force them to be included (if available) so their values are known.
                requestBody.contentType()?.let {
                    if (headers["Content-Type"] == null) {
                        log("Content-Type: $it")
                    }
                }
                if (requestBody.contentLength() != -1L) {
                    if (headers["Content-Length"] == null) {
                        log("Content-Length: ${requestBody.contentLength()}")
                    }
                }
            }

            for (i in 0 until headers.size) {
                logHeader(headers, i)
            }
        }

        if (requestBody == null) {

            log("--> END ${request.method}")
        } else if (bodyHasUnknownEncoding(request.headers)) {
            log("--> END ${request.method} (encoded body omitted)")
        } else if (requestBody.isDuplex()) {
            log("--> END ${request.method} (duplex request body omitted)")
        } else if (requestBody.isOneShot()) {
            log("--> END ${request.method} (one-shot body omitted)")
        } else {
            val buffer = Buffer()
            requestBody.writeTo(buffer)

            val contentType = requestBody.contentType()
            val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

            log("")
            if (buffer.isProbablyUtf8()) {
                log(buffer.readString(charset))
                log("--> END ${request.method} (${requestBody.contentLength()}-byte body)")
            } else {
                log(
                    "--> END ${request.method} (binary ${requestBody.contentLength()}-byte body omitted)")
            }
        }


        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            log("<-- HTTP FAILED: $e")
            throw e
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBody = response.body!!
        val contentLength = responseBody.contentLength()
        val bodySize = if (contentLength != -1L) "$contentLength-byte" else "unknown-length"
        log(
            "<-- ${response.code}${if (response.message.isEmpty()) "" else ' ' + response.message} ${response.request.url} (${tookMs}ms${if (!logHeaders) ", $bodySize body" else ""})")

        val headers = response.headers
        if (logHeaders) {
            for (i in 0 until headers.size) {
                logHeader(headers, i)
            }
        }

        if (!response.promisesBody()) {
            log("<-- END HTTP")
        } else if (bodyHasUnknownEncoding(response.headers)) {
            log("<-- END HTTP (encoded body omitted)")
        } else {
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            var buffer = source.buffer

            var gzippedLength: Long? = null
            if ("gzip".equals(headers["Content-Encoding"], ignoreCase = true)) {
                gzippedLength = buffer.size
                GzipSource(buffer.clone()).use { gzippedResponseBody ->
                    buffer = Buffer()
                    buffer.writeAll(gzippedResponseBody)
                }
            }

            val contentType = responseBody.contentType()
            val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

            if (!buffer.isProbablyUtf8()) {
                log("")
                log("<-- END HTTP (binary ${buffer.size}-byte body omitted)")
                return response
            }

            if (contentLength != 0L) {
                log("")
                log(buffer.clone().readString(charset))
            }

            if (gzippedLength != null) {
                log("<-- END HTTP (${buffer.size}-byte, $gzippedLength-gzipped-byte body)")
            } else {
                log("<-- END HTTP (${buffer.size}-byte body)")

            }
        }

        return response
    }

    private fun logHeader(headers: Headers, i: Int) {
        val value = if (headers.name(i) in headersToRedact) "██" else headers.value(i)

        log(headers.name(i) + ": " + value)

    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"] ?: return false
        return !contentEncoding.equals("identity", ignoreCase = true) &&
                !contentEncoding.equals("gzip", ignoreCase = true)
    }

    private fun Buffer.isProbablyUtf8(): Boolean {
        try {
            val prefix = Buffer()
            val byteCount = size.coerceAtMost(64)
            copyTo(prefix, 0, byteCount)
            for (i in 0 until 16) {
                if (prefix.exhausted()) {
                    break
                }
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            return true
        } catch (_: EOFException) {
            return false
        }
    }

    private fun log(message: String) {
        val length = message.length
        var i = 0
        while (i < length) {
            var newLine = message.indexOf('\n', i)
            newLine = if (newLine != -1) newLine else length
            do {
                val end = newLine.coerceAtMost(i + MAX_LOG_LENGTH)
                logger.log(message.substring(i, end))
                i = end
            } while (i < newLine)

            i++
        }
    }

    companion object {

        private const val MAX_LOG_LENGTH = 2048
    }

}