package net.maxsmr.core.network.api.notification_reader

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.commonutils.text.EMPTY_STRING

@Serializable
data class AppInfo(
    @SerialName("package_name")
    val packageName: String = EMPTY_STRING,
    @SerialName("app_name_prefix")
    val appNamePrefix: String = EMPTY_STRING,
)