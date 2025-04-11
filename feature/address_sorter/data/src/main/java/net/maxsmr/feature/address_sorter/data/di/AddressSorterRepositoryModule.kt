package net.maxsmr.feature.address_sorter.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import net.maxsmr.core.database.dao.address_sorter.AddressDao
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.network.api.GeocodeDataSource
import net.maxsmr.core.network.api.SuggestDataSource
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import net.maxsmr.feature.address_sorter.data.repository.AddressRepoImpl
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
object AddressSorterRepositoryModule {

    @[Provides Singleton]
    fun providesAddressRepository(
        @ApplicationContext context: Context,
        dao: AddressDao,
        settingsRepo: SettingsDataStoreRepository,
    ): AddressRepo {
        return AddressRepoImpl(
            context,
            dao,
            settingsRepo
        )
    }
}