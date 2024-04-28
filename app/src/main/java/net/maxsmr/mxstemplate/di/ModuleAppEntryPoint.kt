package net.maxsmr.mxstemplate.di

import com.squareup.picasso.Picasso
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import net.maxsmr.core.di.BaseJson
import net.maxsmr.mxstemplate.db.AppDataBase
import net.maxsmr.core.di.DI_NAME_VERSION_NAME
import net.maxsmr.core.di.RadarIoSessionStorage
import net.maxsmr.core.network.SessionStorage
import net.maxsmr.mxstemplate.manager.UUIDManager
import javax.inject.Named

@[EntryPoint
InstallIn(SingletonComponent::class)]
internal interface ModuleAppEntryPoint {

    @Named(DI_NAME_VERSION_NAME)
    fun appVersion(): String

    fun database(): AppDataBase

    @BaseJson
    fun baseJson(): Json

    fun picasso(): Picasso

    fun uuidManager(): UUIDManager

    @RadarIoSessionStorage
    fun sessionStorage(): SessionStorage
}