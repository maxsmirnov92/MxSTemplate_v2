package net.maxsmr.core.network.okhttp.body

import net.maxsmr.core.ProgressListener
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import java.io.IOException

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
        } catch (_: IOException) {
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
            listener.notifyWithCheckOrThrow(
                totalBytesWritten,
                contentLength,
                startTime
            )
        }
    }
}