package net.maxsmr.core.network.client.okhttp.vmoscloud

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import net.maxsmr.commonutils.toHexString

internal object PaasSignUtils {

    internal fun signature(
        contentType: String,
        signedHeaders: String,
        host: String,
        xDate: String,
        sk: String,
        service: String,
        body: ByteArray,
    ): String {
        val xContentSha256 = hashSHA256(body)
        val shortXDate = xDate.substring(0, 8)

        val canonicalStringBuilder =
            "host:$host\nx-date:$xDate\ncontent-type:$contentType\nsignedHeaders:$signedHeaders\nx-content-sha256:$xContentSha256"

        val hashCanonicalString = hashSHA256(canonicalStringBuilder.toByteArray())

        val credentialScope = ("$shortXDate/$service").toString() + "/request"
        val signString = "HMAC-SHA256\n$xDate\n$credentialScope\n$hashCanonicalString"

        val signKey = genSigningSecretKeyV4(sk, shortXDate, service)
        return hmacSHA256(signKey, signString).toHexString()
    }

    private fun hashSHA256(content: ByteArray): String {
        try {
            val md = MessageDigest.getInstance("SHA-256")
            return md.digest(content).toHexString()
        } catch (e: Exception) {
            throw Exception("Unable to compute hash while signing request: " + e.message, e)
        }
    }

    private fun hmacSHA256(key: ByteArray?, content: String): ByteArray {
        try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            return mac.doFinal(content.toByteArray())
        } catch (e: Exception) {
            throw Exception("Unable to calculate a request signature: " + e.message, e)
        }
    }

    private fun genSigningSecretKeyV4(secretKey: String, date: String, service: String): ByteArray {
        val kDate = hmacSHA256((secretKey).toByteArray(), date)
        val kService = hmacSHA256(kDate, service)
        return hmacSHA256(kService, "request")
    }
}