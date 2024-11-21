package net.maxsmr.notification_reader.initializers

import android.content.Context
import androidx.startup.Initializer
import kotlinx.coroutines.runBlocking
import net.maxsmr.commonutils.isFileExists
import net.maxsmr.core.domain.entities.feature.settings.AppSettings
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.notification_reader.di.InitializerEntryPoint
import javax.inject.Inject

class AppSettingsInitializer : Initializer<Unit> {

    @Inject
    lateinit var settingsRepo: SettingsDataStoreRepository

    override fun create(context: Context) {
        InitializerEntryPoint.resolve(context).inject(this)
        // scope.launch использовать не вариант, т.к. сразу нужны актуальные значения
        runBlocking {
            if (!isFileExists("datastore/settings.preferences_pb", context.filesDir.absolutePath)) {
                settingsRepo.updateSettings(
                    AppSettings(
                        // дефолтные значения сюда
                    )
                )
            }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf()
}