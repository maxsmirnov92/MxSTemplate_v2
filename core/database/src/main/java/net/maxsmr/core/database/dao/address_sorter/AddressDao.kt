package net.maxsmr.core.database.dao.address_sorter

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import net.maxsmr.core.database.dao.UpsertDao
import net.maxsmr.core.database.model.address_sorter.AddressEntity

@Dao
abstract class AddressDao : UpsertDao<AddressEntity>() {

    @Query("SELECT * FROM address ORDER BY sortOrder ASC, id ASC")
    abstract fun get(): Flow<List<AddressEntity>>

    @Query("SELECT * FROM address ORDER BY sortOrder ASC, id ASC")
    abstract suspend fun getRaw(): List<AddressEntity>

    @Query("SELECT * FROM address WHERE id=:id")
    abstract suspend fun getById(id: Long): AddressEntity?

    @Query("SELECT count(*) FROM address")
    abstract suspend fun count(): Long

    @Query("DELETE FROM address WHERE id=:id")
    abstract suspend fun deleteById(id: Long)

    @Query("DELETE FROM address")
    abstract suspend fun clear()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'address'")
    abstract suspend fun resetAutoIncrement()

    @Transaction
    open suspend fun upsertWithReset(addressEntity: AddressEntity) {
        if (count() == 0L) {
            // FIXME сброс счётчика не работает
            resetAutoIncrement()
            insert(addressEntity)
        } else {
            upsert(addressEntity)
        }
    }
}