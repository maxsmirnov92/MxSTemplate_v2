package net.maxsmr.preferences.repository

import androidx.datastore.core.DataStore
import net.maxsmr.core.di.DataStoreType
import net.maxsmr.core.di.DataStores
import net.maxsmr.preferences.domain.AppSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStoreRepository @Inject constructor(
    @DataStores(DataStoreType.SETTINGS) private val dataStore: DataStore<AppSettings>,
) {

    val settings = dataStore.data

    suspend fun updateSettings(settings: AppSettings) {
        dataStore.updateData { settings }
    }
}