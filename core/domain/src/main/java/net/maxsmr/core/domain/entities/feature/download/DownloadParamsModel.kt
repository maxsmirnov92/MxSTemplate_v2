package net.maxsmr.core.domain.entities.feature.download

import kotlinx.serialization.Serializable
import net.maxsmr.core.domain.entities.feature.network.Method

@Serializable
data class DownloadParamsModel(
    val url: String = "",
    val method: Method = Method.GET,
    val bodyUri: String? = null,
    val fileName: String? = null,
    val ignoreFileName: Boolean = false,
    val subDirName: String? = null,
    val targetMd5Hash: String? = null,
    val ignoreServerErrors: Boolean = false,
    val ignoreAttachment: Boolean = false,
    val replaceFile: Boolean = false,
    val deleteUnfinished: Boolean = true,
    val headers: HashMap<String, String> = hashMapOf(),
)