package com.youtubeapis.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey val url: String,
    val fileName: String,
    val filePath: String,
    val totalSize: Long, // totalBytes
    val downloaded: Long, // downloadedBytes
    val status: String, // downloading, paused, completed
    val speed: Long, // bytes/sec
    val remainingTime: Long // seconds
)
*/

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val url: String,
    val fileName: String,
    val filePath: String,
    val status: String, //1"downloading",1 "paused",0 "completed", etc.
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val mimeType: String? = null,
    val extension: String? = null,
    val speed: Long = 0L,
    val eta: Long = 0L,
    val createdAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
