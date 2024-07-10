package net.maxsmr.core.di

import javax.inject.Qualifier

const val DI_NAME_VERSION_CODE = "version_code"
const val DI_NAME_VERSION_NAME = "version_name"
const val DI_NAME_MAIN_ACTIVITY_CLASS = "main_activity_class"
const val DI_NAME_RATE_APP_ASK_INTERVAL = "rate_app_ask_interval"

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Preferences(val type: PreferencesType)

enum class PreferencesType {
    APP, PERMISSIONS, SESSION_RADAR_IO
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DataStores(val type: DataStoreType)

enum class DataStoreType(val dataStoreName: String) {
    SETTINGS("settings"), CACHE("cache")
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val appDispatchers: AppDispatchers)

enum class AppDispatchers {
    Default,
    IO,
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class BaseJson

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RadarIoHostManager

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RadarIoSessionStorage

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
annotation class DownloadHttpLoggingInterceptor

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PicassoHttpLoggingInterceptor

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RadarIoRetrofit
