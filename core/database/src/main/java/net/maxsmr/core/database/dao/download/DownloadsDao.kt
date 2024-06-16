package net.maxsmr.core.database.dao.download

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.maxsmr.core.database.dao.UpsertDao
import net.maxsmr.core.database.model.download.DownloadInfo

@Dao
abstract class DownloadsDao: UpsertDao<DownloadInfo>() {

    @Query("select * from DownloadInfo")
    abstract fun getAll(): Flow<List<DownloadInfo>>

    @Query("select * from DownloadInfo where name IN (:names)")
    abstract fun getAllByNames(names: List<String>): Flow<List<DownloadInfo>>

    @Query("select * from DownloadInfo")
    abstract suspend fun getAllRaw(): List<DownloadInfo>

    @Query("select * from DownloadInfo where id = :id")
    abstract suspend fun getById(id: Long): DownloadInfo?

    @Query("select * from DownloadInfo where name = :name AND extension = :ext")
    abstract suspend fun getByName(name: String, ext: String): DownloadInfo?

    @Query("select * from DownloadInfo where name = :name and extension = :ext")
    abstract suspend fun getByNameAndExt(name: String, ext: String): DownloadInfo?

    @Query("delete from DownloadInfo")
    abstract suspend fun removeAll()

    @Query("delete from DownloadInfo where id in (:ids)")
    abstract suspend fun remove(vararg ids: Long)

    @Query("delete from DownloadInfo where name = :name AND extension = :ext")
    abstract suspend fun remove(name: String, ext: String)

//    @Query("delete from DownloadInfo where isLoading")
//    abstract suspend fun removeUnfinished()
//
//    @Query("delete from DownloadInfo where NOT isLoading")
//    abstract suspend fun removeFinished()
}