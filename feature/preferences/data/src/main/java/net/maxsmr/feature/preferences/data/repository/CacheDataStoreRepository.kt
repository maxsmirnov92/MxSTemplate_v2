package net.maxsmr.feature.preferences.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.di.DataStoreType
import net.maxsmr.core.di.DataStores
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.utils.decodeFromStringOrNull
import net.maxsmr.core.utils.encodeToStringOrNull
import javax.inject.Inject

/**
 * Репо для хранения кэшированных значений в аппе
 */
class CacheDataStoreRepository @Inject constructor(
    @DataStores(DataStoreType.CACHE) private val dataStore: DataStore<Preferences>,
    @BaseJson private val json: Json,
) {

    val postNotificationAsked = dataStore.data.map { it[FIELD_POST_NOTIFICATION_ASKED] ?: false }

    suspend fun isPostNotificationAsked() = postNotificationAsked.firstOrNull() ?: false

    suspend fun setPostNotificationAsked() {
        dataStore.edit { prefs ->
            prefs[FIELD_POST_NOTIFICATION_ASKED] = true
        }
    }

    // mapNotNull == висяк
    suspend fun getLastLocation(): Address.Location? {
        return dataStore.data.map { prefs ->
            prefs[FIELD_LAST_LOCATION]?.let {
                json.decodeFromStringOrNull(it) as Address.Location?
            }
        }.firstOrNull()
    }

    suspend fun setLastLocation(location: Address.Location?) {
        val result: String = location?.let {
            json.encodeToStringOrNull(location)
        }.orEmpty()
        dataStore.edit { prefs ->
            prefs[FIELD_LAST_LOCATION] = result
        }

    }

    suspend fun getLastQueueId(): Int {
        return dataStore.data.map { prefs ->
            prefs[FIELD_LAST_QUEUE_ID]
        }.firstOrNull() ?: 0
    }

    suspend fun setLastQueueId(id: Int) {
        dataStore.edit { prefs ->
            prefs[FIELD_LAST_QUEUE_ID] = id
        }
    }

    suspend fun hasDownloadParamsModelSample(): Boolean {
        return dataStore.data.map { prefs ->
            prefs[FIELD_HAS_DOWNLOAD_PARAMS_MODEL_SAMPLE]
        }.firstOrNull() ?: false
    }

    suspend fun setHasDownloadParamsModelSample() {
        dataStore.edit { prefs ->
            prefs[FIELD_HAS_DOWNLOAD_PARAMS_MODEL_SAMPLE] = true
        }
    }

    companion object {

        private val FIELD_POST_NOTIFICATION_ASKED = booleanPreferencesKey("postNotificationAsked")
        private val FIELD_LAST_LOCATION = stringPreferencesKey("lastLocation")
        private val FIELD_LAST_QUEUE_ID = intPreferencesKey("lastQueueId")
        private val FIELD_HAS_DOWNLOAD_PARAMS_MODEL_SAMPLE = booleanPreferencesKey("hasDownloadParamsModelSample")
    }
}