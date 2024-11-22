package net.maxsmr.notification_reader.di.modules

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.di.DI_NAME_FOREGROUND_SERVICE_ID_DOWNLOAD
import net.maxsmr.core.di.DI_NAME_FOREGROUND_SERVICE_ID_NOTIFICATION_READER
import net.maxsmr.core.di.DI_NAME_DEMO_PERIOD
import net.maxsmr.core.di.DI_NAME_FOREGROUND_SERVICE_ID_DOWNLOAD
import net.maxsmr.core.di.DI_NAME_IS_DEMO_BUILD
import net.maxsmr.core.di.DI_NAME_MAIN_ACTIVITY_CLASS
import net.maxsmr.core.di.Preferences
import net.maxsmr.core.di.PreferencesType
import net.maxsmr.notification_reader.BuildConfig
import net.maxsmr.notification_reader.manager.UUIDManager
import net.maxsmr.permissionchecker.PermissionsHelper
import net.maxsmr.permissionchecker.PrefsStorage
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class AppModule {

    @[Provides Singleton]
    fun provideUUIDManager(
        @Preferences(PreferencesType.APP) sharedPreferences: SharedPreferences
    ) = UUIDManager(sharedPreferences)

    @[Provides Singleton Named(DI_NAME_MAIN_ACTIVITY_CLASS)]
    fun provideMainActivityClassName(): String = "net.maxsmr.notification_reader.ui.activity.MainBottomActivity"

    @[Provides Singleton Named(DI_NAME_FOREGROUND_SERVICE_ID_DOWNLOAD)]
    fun provideDownloadServiceForegroundId(): Int = -1

    @[Provides Singleton Named(DI_NAME_FOREGROUND_SERVICE_ID_NOTIFICATION_READER)]
    fun provideNotificationReaderServiceForegroundId(): Int = -2

    @[Provides Singleton Named(DI_NAME_IS_DEMO_BUILD)]
    fun provideIsDemoBuild(): Boolean = BuildConfig.IS_DEMO_BUILD

    @[Provides Singleton Named(DI_NAME_DEMO_PERIOD)]
    fun provideDemoPeriod(): Long = TimeUnit.MINUTES.toMillis(5)

    @[Provides Singleton]
    fun providePermissionHelper(
        @Preferences(PreferencesType.PERMISSIONS) sharedPreferences: SharedPreferences
    ) = PermissionsHelper(object : PrefsStorage {
        override val allKeys: Set<String>
            get() = sharedPreferences.all.keys

        override fun containsKey(key: String): Boolean = sharedPreferences.contains(key)

        override fun putBoolean(keys: Map<String, Boolean>) {
            val editor = sharedPreferences.edit()
            keys.forEach {
                editor.putBoolean(it.key, it.value)
            }
            editor.apply()
        }

        override fun removeKeys(keys: Collection<String>) {
            val editor = sharedPreferences.edit()
            keys.forEach {
                editor.remove(it)
            }
            editor.apply()
        }
    })
}