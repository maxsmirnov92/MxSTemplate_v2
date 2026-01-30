package net.maxsmr.core.network

import net.maxsmr.core.ProgressListener
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.IOException

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
            listener.notifyWithCheckOrThrow(
                totalBytesRead,
                contentLength,
                startTime
            )
            return bytesRead
        }
    }
}