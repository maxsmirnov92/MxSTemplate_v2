package net.maxsmr.preferences.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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

    suspend fun getLastLocation(): Address.Location? {
        return dataStore.data.mapNotNull { prefs ->
            prefs[FIELD_LAST_LOCATION]?.let {
                json.decodeFromStringOrNull(it) as Address.Location?
            }
        }.firstOrNull()
    }

    suspend fun setPostNotificationAsked() {
        dataStore.edit { prefs ->
            prefs[FIELD_POST_NOTIFICATION_ASKED] = true
        }
    }

    suspend fun setLastLocation(location: Address.Location?) {
        val result: String = location?.let {
            json.encodeToStringOrNull(location)
        }.orEmpty()
        dataStore.edit { prefs ->
            prefs[FIELD_LAST_LOCATION] = result
        }

    }

    companion object {

        private val FIELD_POST_NOTIFICATION_ASKED = booleanPreferencesKey("postNotificationAsked")
        private val FIELD_LAST_LOCATION = stringPreferencesKey("lastLocation")
    }
}