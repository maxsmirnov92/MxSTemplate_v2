package net.maxsmr.mxstemplate.di

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import net.maxsmr.mxstemplate.initializers.RetrofitInitializer

@[EntryPoint
InstallIn(SingletonComponent::class)]
interface InitializerEntryPoint {

    fun inject(initializer: RetrofitInitializer)

    companion object {

        fun resolve(context: Context): InitializerEntryPoint {
            val appContext = context.applicationContext ?: throw IllegalStateException()
            return EntryPointAccessors.fromApplication(
                appContext,
                InitializerEntryPoint::class.java
            )
        }
    }
}