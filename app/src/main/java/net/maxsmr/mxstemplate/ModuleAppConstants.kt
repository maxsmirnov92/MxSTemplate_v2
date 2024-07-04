package net.maxsmr.mxstemplate

import dagger.hilt.android.EntryPointAccessors
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.mobile_services.MobileBuildType
import net.maxsmr.mxstemplate.di.MobileServicesFeatureEntryPoint
import net.maxsmr.mxstemplate.di.ModuleAppEntryPoint

val dataBase by lazy {
    EntryPointAccessors.fromApplication(baseApplicationContext, ModuleAppEntryPoint::class.java).database()
}

val baseJson by lazy {
    EntryPointAccessors.fromApplication(baseApplicationContext, ModuleAppEntryPoint::class.java).baseJson()
}

val mobileServicesAvailability by lazy {
    EntryPointAccessors.fromApplication(
        baseApplicationContext,
        MobileServicesFeatureEntryPoint::class.java
    ).mobileServicesAvailability
}

val mobileBuildType by lazy {
    MobileBuildType.resolve(BuildConfig.MOBILE_BUILD_TYPE)
}