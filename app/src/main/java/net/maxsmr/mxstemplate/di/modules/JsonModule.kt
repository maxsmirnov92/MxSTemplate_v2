package net.maxsmr.mxstemplate.di.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import net.maxsmr.core.di.BaseJson
import net.maxsmr.feature.webview.ui.WebViewCustomizer
import javax.inject.Singleton
import kotlinx.serialization.modules.subclass


@[Module
InstallIn(SingletonComponent::class)]
class JsonModule {

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    @BaseJson
    fun provideBaseJson(): Json = Json {
        serializersModule  = SerializersModule {
            polymorphic(WebViewCustomizer.ViewUrlStrategy.UrlMatcher::class) {
                subclass(WebViewCustomizer.ViewUrlStrategy.UrlMatcher.AnyUrlMatcher::class)
                subclass(WebViewCustomizer.ViewUrlStrategy.UrlMatcher.PartsUrlMatcher::class)
            }
        }
        allowSpecialFloatingPointValues = true
        // исключать поля с нульными значениями
        explicitNulls = false
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
}