package net.maxsmr.feature.preferences.data.repository

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.firstOrNull
import net.maxsmr.core.di.DataStoreType
import net.maxsmr.core.di.DataStores
import net.maxsmr.feature.preferences.data.domain.AppSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStoreRepository @Inject constructor(
    @DataStores(DataStoreType.SETTINGS) private val dataStore: DataStore<AppSettings>,
) {

    val settingsFlow = dataStore.data

    suspend fun getSettings() = settingsFlow.firstOrNull() ?: AppSettings()

    suspend fun updateSettings(settings: AppSettings) {
        dataStore.updateData { settings }
    }
}