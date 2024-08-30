package net.maxsmr.core.database.dao

import androidx.room.*
import java.util.*

/**
 * Дао с добавлением операции upsert (update or insert).
 * Имеет смысл наследовать от нее ДАО для родительских таблиц с наличием CASCADE ForeignKey связей, т.к.
 * при использовании insert(OnConflictStrategy.REPLACE) записи из зависимых таблиц будут каскадно удалены.
 * insert-методы используют OnConflictStrategy.IGNORE.
 */
abstract class UpsertDao<T : Any> {

    @Insert
    abstract suspend fun insert(obj: T)

    @Update
    abstract suspend fun update(obj: T)

    @Update
    abstract suspend fun update(obj: List<T>)

    @Delete
    abstract suspend fun delete(obj: T)

    /**
     * Создает запись в таблице, либо обновляет существующую, если уже есть с таким первичным ключом
     */
    @Upsert
    abstract suspend fun upsert(obj: T): Long

    /**
     * Создает записи в таблице, либо обновляет существующие, если уже есть с таким первичным ключом
     */
    @Upsert
    abstract suspend fun upsert(objList: List<T>): List<Long>

    companion object {

        const val NO_ID = -1L
    }
}