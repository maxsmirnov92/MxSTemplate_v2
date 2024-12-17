package net.maxsmr.notification_reader.di.modules

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.di.DI_NAME_DATABASE_NAME
import net.maxsmr.notification_reader.db.AppDataBase
import javax.inject.Named
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class DatabaseModule {

    @[Provides Singleton]
    fun dataBase(
        @ApplicationContext context: Context,
    ): AppDataBase =
        Room.databaseBuilder(context, AppDataBase::class.java, "appRoomDataBase")
//            .addMigrations(*Migrations.get())
//            .allowMainThreadQueries()
            .build()

    @[Named(DI_NAME_DATABASE_NAME) Provides Singleton]
    fun dataBaseName(database: AppDataBase): String = database.openHelper.databaseName.orEmpty()
}