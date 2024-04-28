package net.maxsmr.mxstemplate

import dagger.hilt.android.EntryPointAccessors
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.mxstemplate.di.ModuleAppEntryPoint

val dataBase by lazy {
    EntryPointAccessors.fromApplication(baseApplicationContext, ModuleAppEntryPoint::class.java).database()
}

val baseJson by lazy {
    EntryPointAccessors.fromApplication(baseApplicationContext, ModuleAppEntryPoint::class.java).baseJson()
}