package net.maxsmr.feature.preferences.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import net.maxsmr.commonutils.stream.readStringOrThrow
import net.maxsmr.commonutils.stream.writeBytesOrThrow
import net.maxsmr.commonutils.writeBytesOrThrow
import net.maxsmr.core.domain.entities.feature.settings.AppSettings
import java.io.InputStream
import java.io.OutputStream

class AppSettingsSerializer(
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