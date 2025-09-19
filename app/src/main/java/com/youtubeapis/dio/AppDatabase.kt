package com.youtubeapis.dio


import androidx.room.RoomDatabase
import com.youtubeapis.model.DownloadEntity


@androidx.room.Database(entities = [DownloadEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}