package net.maxsmr.feature.preferences.data.repository

import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.di.DataStoreType
import net.maxsmr.core.di.DataStores
import net.maxsmr.core.domain.entities.feature.rate.RateAppInfo
import net.maxsmr.core.utils.kotlinx.serialization.decodeFromStringOrNull
import net.maxsmr.core.utils.kotlinx.serialization.encodeToStringOrNull
import javax.inject.Inject

/**
 * Репо для хранения кэшированных значений в аппе
 */
class CacheDataStoreRepository @Inject constructor(
    @DataStores(DataStoreType.CACHE) private val dataStore: DataStore<Preferences>,
    @BaseJson private val json: Json,
) {

    private val data: Flow<Preferences> = dataStore.data

    val postNotificationAsked: Flow<Boolean>? = data.map { it[FIELD_POST_NOTIFICATION_ASKED] ?: false }
        .takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }

    val batteryOptimizationAsked: Flow<Boolean> = data.map { it[FIELD_BATTERY_OPTIMIZATION_ASKED] ?: false }

    val isDemoPeriodExpired: Flow<Boolean> = data.map { it[FIELD_KEY_DEMO_PERIOD_EXPIRED] ?: false }

    val isTutorialCompleted: Flow<Boolean> = data.map { it[FIELD_KEY_TUTORIAL_COMPLETED] ?: false }

    suspend fun wasPostNotificationAsked() = postNotificationAsked?.firstOrNull() ?: false

    suspend fun setPostNotificationAsked() {
        setPostNotificationAsked(true)
    }

    suspend fun clearPostNotificationAsked() {
        setPostNotificationAsked(false)
    }

    private suspend fun setPostNotificationAsked(toggle: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            dataStore.edit { prefs ->
                prefs[FIELD_POST_NOTIFICATION_ASKED] = toggle
            }
        }
    }

    suspend fun wasBatteryOptimizationAsked(): Boolean {
        return dataStore.data.map { prefs ->
            prefs[FIELD_BATTERY_OPTIMIZATION_ASKED]
        }.firstOrNull() ?: false
    }

    suspend fun setBatteryOptimizationAsked() {
        dataStore.edit { prefs ->
            prefs[FIELD_BATTERY_OPTIMIZATION_ASKED] = true
        }
    }

    suspend fun getLastQueueId(): Int {
        // mapNotNull == висяк
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

    suspend fun getAppRateInfo(): RateAppInfo {
        return dataStore.data.map { prefs ->
            prefs[FIELD_RATE_APP_INFO]?.let {
                json.decodeFromStringOrNull(it) as RateAppInfo?
            }
        }.firstOrNull() ?: RateAppInfo(false, notAskAgain = false).also {
            setRateAppInfo(it)
        }
    }

    suspend fun setAppRated() {
        setRateAppInfo(RateAppInfo(true, notAskAgain = true))
    }

    suspend fun setAppNotRated(notAskAgain: Boolean) {
        setRateAppInfo(RateAppInfo(false, notAskAgain))
    }

    private suspend fun setRateAppInfo(rateInfo: RateAppInfo) {
        val result: String =
            json.encodeToStringOrNull(rateInfo).orEmpty()
        dataStore.edit { prefs ->
            prefs[FIELD_RATE_APP_INFO] = result
        }
    }

    suspend fun getSeenReleaseNotesVersionCodes(): List<Int> {
        val jsonArray = dataStore.data.map { prefs ->
            prefs[FIELD_SEEN_RELEASE_NOTES_VERSION_CODES]
        }.firstOrNull()
        return json.decodeFromStringOrNull<List<Int>>(jsonArray).orEmpty()
    }

    suspend fun setSeenReleaseNotesVersionCode(id: Int) {
        val currentCodes = getSeenReleaseNotesVersionCodes().toMutableSet()
        currentCodes.add(id)
        dataStore.edit { prefs ->
            prefs[FIELD_SEEN_RELEASE_NOTES_VERSION_CODES] = json.encodeToStringOrNull(currentCodes.toList()).orEmpty()
        }
    }

    suspend fun getLastCheckInAppUpdate(): Long {
        return dataStore.data.map { prefs ->
            prefs[FIELD_LAST_CHECK_IN_APP_UPDATE]
        }.firstOrNull() ?: 0L
    }

    suspend fun setCurrentLastCheckInAppUpdate() {
        setLastCheckInAppUpdate(System.currentTimeMillis())
    }

    suspend fun clearLastCheckInAppUpdate() {
        setLastCheckInAppUpdate(0)
    }

    private suspend fun setLastCheckInAppUpdate(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[FIELD_LAST_CHECK_IN_APP_UPDATE] = timestamp
        }
    }

    suspend fun getDoubleGisRoutingApiKey(): String {
        return dataStore.data.map { prefs ->
            prefs[FIELD_KEY_DOUBLE_GIS_ROUTING_API_KEY]
        }.firstOrNull().orEmpty()
    }

    suspend fun setDoubleGisRoutingApiKey(key: String) {
        dataStore.edit { prefs ->
            prefs[FIELD_KEY_DOUBLE_GIS_ROUTING_API_KEY] = key
        }
    }

    suspend fun isDemoPeriodExpired() = isDemoPeriodExpired.firstOrNull() ?: false

    suspend fun setDemoPeriodExpired() {
        setDemoPeriodExpired(true)
    }

    suspend fun clearDemoPeriodExpired() {
        setDemoPeriodExpired(false)
    }

    private suspend fun setDemoPeriodExpired(toggle: Boolean) {
        dataStore.edit { prefs ->
            prefs[FIELD_KEY_DEMO_PERIOD_EXPIRED] = toggle
        }
    }

    suspend fun isTutorialCompeted(): Boolean {
        return dataStore.data.map { prefs ->
            prefs[FIELD_KEY_TUTORIAL_COMPLETED]
        }.firstOrNull() ?: false
    }

    suspend fun setTutorialCompleted(toggle: Boolean) {
        dataStore.edit { prefs ->
            prefs[FIELD_KEY_TUTORIAL_COMPLETED] = toggle
        }
    }

    companion object {

        private val FIELD_POST_NOTIFICATION_ASKED = booleanPreferencesKey("postNotificationAsked")
        private val FIELD_BATTERY_OPTIMIZATION_ASKED = booleanPreferencesKey("batteryOptimizationAsked")
        private val FIELD_LAST_QUEUE_ID = intPreferencesKey("lastQueueId")
        private val FIELD_HAS_DOWNLOAD_PARAMS_MODEL_SAMPLE = booleanPreferencesKey("hasDownloadParamsModelSample")
        private val FIELD_RATE_APP_INFO = stringPreferencesKey("rateAppInfo")
        private val FIELD_SEEN_RELEASE_NOTES_VERSION_CODES = stringPreferencesKey("seenReleaseNotesVersionCodes")
        private val FIELD_LAST_CHECK_IN_APP_UPDATE = longPreferencesKey("lastCheckInAppUpdate")
        private val FIELD_KEY_DOUBLE_GIS_ROUTING_API_KEY = stringPreferencesKey("keyDoubleGisRoutingApiKey")
        private val FIELD_KEY_DEMO_PERIOD_EXPIRED = booleanPreferencesKey("demoPeriodExpired")
        private val FIELD_KEY_TUTORIAL_COMPLETED = booleanPreferencesKey("tutorialCompleted")
    }
}