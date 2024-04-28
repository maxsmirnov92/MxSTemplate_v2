package net.maxsmr.mxstemplate.di.modules

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.di.DataStoreType
import net.maxsmr.core.di.DataStores
import net.maxsmr.core.di.Preferences
import net.maxsmr.core.di.PreferencesType
import net.maxsmr.core.network.SessionStorage
import net.maxsmr.mxstemplate.session.RadarIoSessionStorage
import net.maxsmr.preferences.domain.AppSettings
import net.maxsmr.preferences.domain.UserPreferencesSerializer
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class StorageModule {

    @[Provides Singleton Preferences(PreferencesType.APP)]
    fun provideAppPrefs(
        @ApplicationContext context: Context,
    ): SharedPreferences = context.getSharedPreferences("prefs_app", Context.MODE_PRIVATE)

    @[Provides Singleton Preferences(PreferencesType.PERMISSIONS)]
    fun providePermissionsPrefs(
        @ApplicationContext context: Context,
    ): SharedPreferences = context.getSharedPreferences("prefs_permissions", Context.MODE_PRIVATE)

    @[Provides Singleton Preferences(PreferencesType.SESSION_RADAR_IO)]
    fun provideRadarIoSessionPrefs(
        @ApplicationContext context: Context,
    ): SharedPreferences = context.getSharedPreferences("prefs_session", Context.MODE_PRIVATE)

    @[Provides Singleton net.maxsmr.core.di.RadarIoSessionStorage]
    fun provideRadarIoSessionStorage(
        @Preferences(PreferencesType.SESSION_RADAR_IO) sharedPreferences: SharedPreferences,
    ): SessionStorage = RadarIoSessionStorage()

    @[Provides Singleton DataStores(DataStoreType.CACHE)]
    fun provideCacheDataStore(
        @ApplicationContext context: Context,
    ): DataStore<androidx.datastore.preferences.core.Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile(DataStoreType.CACHE.dataStoreName)
    }

    @[Provides Singleton DataStores(DataStoreType.SETTINGS)]
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
        @BaseJson json: Json,
    ): DataStore<AppSettings> = DataStoreFactory.create(
        serializer = UserPreferencesSerializer(json),
        produceFile = { context.preferencesDataStoreFile(DataStoreType.SETTINGS.dataStoreName) },
    )
}