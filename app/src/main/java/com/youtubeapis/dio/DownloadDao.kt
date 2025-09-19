package com.youtubeapis.dio

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.youtubeapis.model.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE status != :status ORDER BY createdAt DESC")
    fun getAllFlow(status: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt DESC")
    fun getAllFlow2(status: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    suspend fun getAll(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getByID(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getByIDLimit(id: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DownloadEntity): Long

    @Update
    suspend fun update(item: DownloadEntity): Int

    @Query("""
    UPDATE downloads 
    SET downloadedBytes = :downloadedBytes, 
        speed = :speed, 
        eta = :eta, 
        updatedAt = :updatedAt 
    WHERE id = :id
""")
    suspend fun updateProgress(id: Long, downloadedBytes: Long, speed: Long, eta: Long, updatedAt: Long)


    @Delete
    suspend fun delete(item: DownloadEntity)
}