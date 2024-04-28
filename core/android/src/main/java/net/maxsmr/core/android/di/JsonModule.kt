package net.maxsmr.core.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import net.maxsmr.core.di.BaseJson
import javax.inject.Singleton


@[Module
InstallIn(SingletonComponent::class)]
class JsonModule {

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    @BaseJson
    fun provideBaseJson(): Json = Json {
        allowSpecialFloatingPointValues = true
        // исключать поля с нульными значениями
        explicitNulls = false
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
}