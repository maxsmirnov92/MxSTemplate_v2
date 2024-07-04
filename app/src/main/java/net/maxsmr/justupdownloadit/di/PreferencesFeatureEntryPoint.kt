package net.maxsmr.justupdownloadit.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository

@[EntryPoint
InstallIn(SingletonComponent::class)]
interface PreferencesFeatureEntryPoint {

    val cacheDataStoreRepository: CacheDataStoreRepository

    val settingsDataStoreRepository: SettingsDataStoreRepository
}