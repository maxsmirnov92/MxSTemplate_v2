package net.maxsmr.notification_reader.di.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.database.dao.download.DownloadsDao
import net.maxsmr.core.database.dao.notification_reader.NotificationReaderDao
import net.maxsmr.notification_reader.db.AppDataBase

@Module
@InstallIn(SingletonComponent::class)
object DaosModule {

    @Provides
    fun providesDownloadsDao(database: AppDataBase): DownloadsDao = database.downloadsDao(database)

    @Provides
    fun providesNotificationReaderDao(database: AppDataBase): NotificationReaderDao = database.notificationReaderDao(database)
}