package net.maxsmr.feature.preferences.data.repository

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import net.maxsmr.core.di.DataStoreType
import net.maxsmr.core.di.DataStores
import javax.inject.Inject

/**
 * Репо для хранения кэшированных значений в аппе
 */
class CacheDataStoreRepository @Inject constructor(
    @DataStores(DataStoreType.CACHE) private val dataStore: DataStore<Preferences>,
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

    suspend fun getNotificationReaderKey(defaultKey: String): String {
        var result = dataStore.data.map { prefs ->
            prefs[FIELD_KEY_NOTIFICATION_READER_API_KEY]
        }.firstOrNull().orEmpty()
        if (result.isEmpty()) {
            setNotificationReaderKey(defaultKey)
            result = defaultKey
        }
        return result
    }

    suspend fun setNotificationReaderKey(key: String) {
        dataStore.edit { prefs ->
            prefs[FIELD_KEY_NOTIFICATION_READER_API_KEY] = key
        }
    }

    suspend fun isPackageInWhiteList(
        context: Context,
        packageName: String,
        isWhiteList: Boolean,
    ): Boolean = getPackagesWhiteList().let {
        context.packageName != packageName && (it.isEmpty()
                || if (isWhiteList) it.contains(packageName) else !it.contains(packageName))
    }

    suspend fun getPackagesWhiteList(): Set<String> {
        return dataStore.data.map { prefs ->
            prefs[FIELD_KEY_PACKAGES_WHITE_LIST]
        }.firstOrNull().orEmpty()
    }

    suspend fun setPackagesWhiteList(packages: Set<String>) {
        dataStore.edit { prefs ->
            prefs[FIELD_KEY_PACKAGES_WHITE_LIST] = packages
        }
    }

    companion object {

        private val FIELD_POST_NOTIFICATION_ASKED = booleanPreferencesKey("postNotificationAsked")
        private val FIELD_BATTERY_OPTIMIZATION_ASKED = booleanPreferencesKey("batteryOptimizationAsked")
        private val FIELD_LAST_QUEUE_ID = intPreferencesKey("lastQueueId")
        private val FIELD_KEY_NOTIFICATION_READER_API_KEY = stringPreferencesKey("keyNotificationReader")
        private val FIELD_KEY_PACKAGES_WHITE_LIST = stringSetPreferencesKey("keyPackagesWhiteList")
        private val FIELD_KEY_DEMO_PERIOD_EXPIRED = booleanPreferencesKey("demoPeriodExpired")
        private val FIELD_KEY_TUTORIAL_COMPLETED = booleanPreferencesKey("tutorialCompleted")
    }
}