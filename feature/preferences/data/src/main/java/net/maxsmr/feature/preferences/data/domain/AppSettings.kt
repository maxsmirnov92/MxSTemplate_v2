package net.maxsmr.feature.preferences.data.domain

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import net.maxsmr.commonutils.readStringOrThrow
import net.maxsmr.commonutils.writeBytesOrThrow
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable

@kotlinx.serialization.Serializable
data class AppSettings(
    val maxDownloads: Int = 3,
    val ignoreServerError: Boolean = false,
    val deleteUnfinished: Boolean = true,
    val disableNotifications: Boolean = false,
): Serializable

class UserPreferencesSerializer(
    private val json: Json,
) : Serializer<AppSettings> {

    override val defaultValue: AppSettings = AppSettings()

    override suspend fun readFrom(input: InputStream): AppSettings {
        return try {
            json.decodeFromString(AppSettings.serializer(), input.readStringOrThrow()!!)
        } catch (e: Exception) {
            throw CorruptionException("Unable to read AppSettings", e)
        }
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        try {
            output.writeBytesOrThrow(
                Json.encodeToString(AppSettings.serializer(), t).encodeToByteArray()
            )
        } catch (e: Exception) {
            throw CorruptionException("Unable to write AppSettings", e)
        }
    }
}