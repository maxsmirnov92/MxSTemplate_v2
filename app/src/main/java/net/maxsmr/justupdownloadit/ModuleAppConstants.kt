package net.maxsmr.justupdownloadit

import dagger.hilt.android.EntryPointAccessors
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.justupdownloadit.di.ModuleAppEntryPoint
import net.maxsmr.mobile_services.MobileBuildType
import java.util.concurrent.TimeUnit

val dataBase by lazy {
    EntryPointAccessors.fromApplication(baseApplicationContext, ModuleAppEntryPoint::class.java).database()
}

val baseJson by lazy {
    EntryPointAccessors.fromApplication(baseApplicationContext, ModuleAppEntryPoint::class.java).baseJson()
}

val uuidManager by lazy {
    EntryPointAccessors.fromApplication(baseApplicationContext, ModuleAppEntryPoint::class.java).uuidManager()
}

val mobileBuildType by lazy {
    MobileBuildType.resolve(BuildConfig.MOBILE_BUILD_TYPE)
}

const val RELEASE_NOTES_ASSETS_FOLDER_NAME_EN = "release_notes"
const val RELEASE_NOTES_ASSETS_FOLDER_NAME_RU = "release_notes_ru"

val RATE_APP_ASK_INTERVAL = TimeUnit.MINUTES.toMillis(30)

val CHECK_IN_APP_UPDATES_INTERVAL = TimeUnit.MINUTES.toMillis(10)
