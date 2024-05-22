package net.maxsmr.core.domain.entities.feature.download

import kotlinx.serialization.Serializable

@Serializable
data class DownloadParamsModel(
    val url: String = "",
    val method: Method = Method.GET,
    val bodyUri: String? = null,
    val fileName: String? = null,
    val ignoreFileName: Boolean = false,
    val subDirName: String = "",
    val targetMd5Hash: String? = null,
    val ignoreServerErrors: Boolean = false,
    val ignoreAttachment: Boolean = false,
    val deleteUnfinished: Boolean = false,
    val headers: HashMap<String, String> = hashMapOf(),
) {

    @Serializable
    enum class Method(val value: String) {

        GET("GET"),
        POST("POST"),
    }
}