package net.maxsmr.justupdownloadit

import dagger.hilt.android.EntryPointAccessors
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.justupdownloadit.di.ModuleAppEntryPoint

val dataBase by lazy {
    EntryPointAccessors.fromApplication(baseApplicationContext, ModuleAppEntryPoint::class.java).database()
}

val baseJson by lazy {
    EntryPointAccessors.fromApplication(baseApplicationContext, ModuleAppEntryPoint::class.java).baseJson()
}