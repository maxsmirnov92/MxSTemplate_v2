package net.maxsmr.core.android.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase

fun Context.truncateTable(databaseName: String, tableName: String) {
    val database = SQLiteDatabase.openOrCreateDatabase(
        getDatabasePath(databaseName),
        null
    )
    database?.let {
        it.execSQL(String.format("DELETE FROM %s;", tableName))
        it.execSQL("UPDATE sqlite_sequence SET seq = 0 WHERE name = ?;", arrayOf(tableName))
    }
}