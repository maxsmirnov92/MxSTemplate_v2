package net.maxsmr.core.di

import javax.inject.Qualifier

const val DI_NAME_MAIN_ACTIVITY_CLASS = "main_activity_class"
const val DI_NAME_IS_DEMO_BUILD = "is_demo_build"
const val DI_NAME_DEMO_PERIOD = "demo_period"
const val DI_NAME_VERSION_CODE = "version_code"
const val DI_NAME_VERSION_NAME = "version_name"
const val DI_NAME_DATABASE_NAME = "database_name"
const val DI_NAME_FOREGROUND_SERVICE_ID_DOWNLOAD = "foreground_service_id_download"

/**
 * Опционально, передать эту экстру с именем класса для понимания, кто вызвал
 */
const val EXTRA_CALLER_CLASS_NAME = "caller_class_name"

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Preferences(val type: PreferencesType)

enum class PreferencesType {
    APP, PERMISSIONS
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DataStores(val type: DataStoreType)

enum class DataStoreType(val dataStoreName: String) {
    SETTINGS("settings"), CACHE("cache")
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class BaseJson

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RadarIoHostManager

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class YandexSuggestHostManager

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class YandexGeocodeHostManager

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DoubleGisRoutingHostManager

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DownloaderOkHttpClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PicassoOkHttpClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RadarIoOkHttpClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class YandexSuggestOkHttpClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class YandexGeocodeOkHttpClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DoubleGisRoutingOkHttpClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DownloadHttpLoggingInterceptor

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PicassoHttpLoggingInterceptor

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RadarIoRetrofit

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class YandexSuggestRetrofit

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class YandexGeocodeRetrofit

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DoubleGisRoutingRetrofit