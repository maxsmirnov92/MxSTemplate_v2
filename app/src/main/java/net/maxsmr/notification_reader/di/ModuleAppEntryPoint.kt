package net.maxsmr.notification_reader.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.di.NotificationReaderRetrofit
import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient
import net.maxsmr.notification_reader.db.AppDataBase
import net.maxsmr.notification_reader.manager.UUIDManager

@[EntryPoint
InstallIn(SingletonComponent::class)]
internal interface ModuleAppEntryPoint {

    fun database(): AppDataBase

    @BaseJson
    fun baseJson(): Json

    fun uuidManager(): UUIDManager

    @NotificationReaderRetrofit
    fun notificationReaderRetrofit(): CommonRetrofitClient
}