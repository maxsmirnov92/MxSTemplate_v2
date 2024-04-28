package net.maxsmr.core.network.retrofit.interceptors

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.internal.connection.RealCall
import okio.Buffer
import org.json.JSONObject
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*

private const val ALGORITHM_DEFAULT = "SHA-1"
private const val HEX_CHARS = "0123456789ABCDEF"

object InterceptorUtils {

    fun calcHash(path: String, deviceGuid: String, sessionId: String, requestHash: String, salt: String): String {
        val sb = StringBuilder().append(path, deviceGuid, sessionId, requestHash, salt)

        val algorithm = MessageDigest.getInstance(ALGORITHM_DEFAULT)
        algorithm.reset()
        algorithm.update(sb.toString().toByteArray(Charsets.UTF_8))
        return algorithm.digest().toHexString()
    }

    private fun ByteArray?.toHexString(): String {
        this ?: return ""

        val result = StringBuilder(size * 2)
        forEach {
            val i = it.toInt()
            result.append(HEX_CHARS[i shr 4 and 0x0f])
            result.append(HEX_CHARS[i and 0x0f])
        }
        return result.toString().lowercase(Locale.getDefault())
    }

    fun String.toJSONObject(): JSONObject = if (this.isNotEmpty()) JSONObject(this) else JSONObject()

    fun Interceptor.Chain.resetTimeout() {
        (this.call() as? RealCall)?.let { realCall ->
            realCall.timeout().exit()
            realCall.timeout().enter()
        }
    }
}