package net.maxsmr.mxstemplate.di.modules

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.di.Preferences
import net.maxsmr.core.di.PreferencesType
import net.maxsmr.mxstemplate.manager.UUIDManager
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