package net.maxsmr.feature.preferences.data.domain

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import net.maxsmr.commonutils.readStringOrThrow
import net.maxsmr.commonutils.writeBytesOrThrow
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import java.util.concurrent.TimeUnit

@kotlinx.serialization.Serializable
data class AppSettings(
    val maxDownloads: Int = 3,
    val connectTimeout: Long = 30,
    val retryOnConnectionFailure: Boolean = true,
    val retryDownloads: Boolean = true,
    val disableNotifications: Boolean = false,
    val updateNotificationInterval: Long = 300,
    val startPageUrl: String = "https://google.com"
): Serializable {

    companion object {

        const val UPDATE_NOTIFICATION_INTERVAL_MIN = 100L
        const val UPDATE_NOTIFICATION_INTERVAL_DEFAULT = 300L
    }
}

class UserPreferencesSerializer(
    private val json: Json,
) : Serializer<AppSettings> {

    override val defaultValue: AppSettings = AppSettings()

    override suspend fun readFrom(input: InputStream): AppSettings {
        return try {
            json.decodeFromString(AppSettings.serializer(), input.readStringOrThrow())
        } catch (e: Exception) {
            throw CorruptionException("Unable to read AppSettings", e)
        }
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        try {
            output.writeBytesOrThrow(
                json.encodeToString(AppSettings.serializer(), t).encodeToByteArray()
            )
        } catch (e: Exception) {
            throw CorruptionException("Unable to write AppSettings", e)
        }
    }
}