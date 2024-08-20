package net.maxsmr.feature.address_sorter.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import net.maxsmr.core.database.dao.address_sorter.AddressDao
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.di.RadarIoRetrofit
import net.maxsmr.core.network.api.radar_io.RadarIoDataSource
import net.maxsmr.core.network.retrofit.client.RadarIoRetrofitClient
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import net.maxsmr.feature.address_sorter.data.repository.AddressRepoImpl
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
object AddressSorterRepositoryModule {

    @[Provides Singleton]
    fun providesAddressRepository(
        dao: AddressDao,
        cacheRepo: CacheDataStoreRepository,
        @BaseJson json: Json,
        @RadarIoRetrofit retrofitClient: RadarIoRetrofitClient,
    ): AddressRepo {
        return AddressRepoImpl(
            dao,
            cacheRepo,
            json,
            RadarIoDataSource(retrofitClient)
        )
    }
}