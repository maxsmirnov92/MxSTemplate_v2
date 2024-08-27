package net.maxsmr.mxstemplate.di

import com.squareup.picasso.Picasso
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.di.RadarIoRetrofit
import net.maxsmr.core.di.YandexGeocodeRetrofit
import net.maxsmr.core.di.YandexSuggestRetrofit
import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient
import net.maxsmr.core.network.client.retrofit.YandexGeocodeRetrofitClient
import net.maxsmr.mxstemplate.db.AppDataBase
import net.maxsmr.mxstemplate.manager.UUIDManager

@[EntryPoint
InstallIn(SingletonComponent::class)]
internal interface ModuleAppEntryPoint {

    fun database(): AppDataBase

    @BaseJson
    fun baseJson(): Json

    fun picasso(): Picasso

    fun uuidManager(): UUIDManager

    @RadarIoRetrofit
    fun radarIoRetrofit(): CommonRetrofitClient

    @YandexSuggestRetrofit
    fun yandexSuggestRetrofit(): CommonRetrofitClient

    @YandexGeocodeRetrofit
    fun yandexGeocodeRetrofit(): YandexGeocodeRetrofitClient
}