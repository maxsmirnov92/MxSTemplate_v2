package net.maxsmr.justupdownloadit.di

import com.squareup.picasso.Picasso
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import net.maxsmr.core.di.BaseJson
import net.maxsmr.justupdownloadit.db.AppDataBase
import net.maxsmr.justupdownloadit.manager.UUIDManager

@[EntryPoint
InstallIn(SingletonComponent::class)]
internal interface ModuleAppEntryPoint {

    fun database(): AppDataBase

    @BaseJson
    fun baseJson(): Json

    fun uuidManager(): UUIDManager
}