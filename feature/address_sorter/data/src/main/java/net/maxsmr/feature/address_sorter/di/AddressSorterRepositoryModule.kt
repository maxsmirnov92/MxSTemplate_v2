package net.maxsmr.feature.address_sorter.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import net.maxsmr.core.database.dao.address_sorter.AddressDao
import net.maxsmr.core.di.AppDispatchers
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.di.Dispatcher
import net.maxsmr.core.di.RadarIoRetrofit
import net.maxsmr.core.network.api.radar_io.RadarIoDataSource
import net.maxsmr.core.network.retrofit.client.RadarIoRetrofitClient
import net.maxsmr.feature.address_sorter.repository.AddressRepo
import net.maxsmr.feature.address_sorter.repository.AddressRepoImpl
import net.maxsmr.preferences.repository.CacheDataStoreRepository
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
object AddressSorterRepositoryModule {

    @[Provides Singleton]
    fun providesAddressRepository(
        dao: AddressDao,
        @Dispatcher(AppDispatchers.IO) ioDispatcher: CoroutineDispatcher,
        cacheRepo: CacheDataStoreRepository,
        @BaseJson json: Json,
        @RadarIoRetrofit retrofitClient: RadarIoRetrofitClient
    ): AddressRepo {
        return AddressRepoImpl(
            dao,
            ioDispatcher,
            cacheRepo,
            json,
            RadarIoDataSource(ioDispatcher, retrofitClient)
        )
    }
}