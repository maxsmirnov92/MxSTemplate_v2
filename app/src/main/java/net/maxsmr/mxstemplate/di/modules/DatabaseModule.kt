package net.maxsmr.mxstemplate.di.modules

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.maxsmr.mxstemplate.db.AppDataBase
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
}