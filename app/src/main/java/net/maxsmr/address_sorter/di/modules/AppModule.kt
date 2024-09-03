package net.maxsmr.address_sorter.di.modules

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.address_sorter.BuildConfig
import net.maxsmr.address_sorter.manager.UUIDManager
import net.maxsmr.core.di.DI_NAME_MAIN_ACTIVITY_CLASS
import net.maxsmr.core.di.DI_NAME_ROUTING_KEY_URL
import net.maxsmr.core.di.Preferences
import net.maxsmr.core.di.PreferencesType
import net.maxsmr.permissionchecker.PermissionsHelper
import net.maxsmr.permissionchecker.PrefsStorage
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
    fun provideMainActivityClassName(): String = "net.maxsmr.mxstemplate.ui.activity.MainBottomActivity"

    @[Provides Singleton Named(DI_NAME_ROUTING_KEY_URL)]
    fun provideRoutingKeyUrl(): String = BuildConfig.URL_DEMO_KEY_DOUBLE_GIS_ROUTING

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