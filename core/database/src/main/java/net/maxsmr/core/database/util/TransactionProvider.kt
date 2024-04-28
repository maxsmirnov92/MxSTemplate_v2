package net.maxsmr.core.database.util

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import javax.inject.Inject

class TransactionProvider @Inject constructor(
    private val db: RoomDatabase,
) {

    suspend fun <R> runAsTransaction(block: suspend () -> R): R {
        return db.withTransaction(block)
    }
}