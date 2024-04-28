package net.maxsmr.mxstemplate.di.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.database.dao.address_sorter.AddressDao
import net.maxsmr.core.database.dao.download.DownloadsDao
import net.maxsmr.mxstemplate.db.AppDataBase

@Module
@InstallIn(SingletonComponent::class)
object DaosModule {

    @Provides
    fun providesDownloadsDao(database: AppDataBase): DownloadsDao = database.downloadsDao(database)

    @Provides
    fun providesAddressDao(database: AppDataBase): AddressDao = database.addressDao(database)
}