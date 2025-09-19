package com.youtubeapis.worker

import android.annotation.SuppressLint
import android.content.Context
import com.youtubeapis.dio.AppDatabase
import com.youtubeapis.model.DownloadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.LinkedList

class DownloadManager private constructor(
    private val context: Context,
    private val db: AppDatabase,
    private val scope: CoroutineScope
) {
    companion object {
        private const val MAX_CONCURRENT_DOWNLOADS = 5

        @SuppressLint("StaticFieldLeak") // Safe: Only storing applicationContext
        @Volatile
        private var instance: DownloadManager? = null

        fun getInstance(context: Context, db: AppDatabase, scope: CoroutineScope): DownloadManager {
            return instance ?: synchronized(this) {
                instance ?: DownloadManager(context.applicationContext, db, scope).also {
                    instance = it
                }
            }
        }
    }

    private var onAllDownloadsFinished: (() -> Unit)? = null

    fun setOnAllDownloadsFinishedCallback(callback: () -> Unit) {
        onAllDownloadsFinished = callback
    }

    private val activeDownloads = mutableMapOf<Long, Job>()
    private val downloadQueue = LinkedList<DownloadEntity>()

    private suspend fun insertDownload(url: String, fileName: String, ext: String, mime: String): DownloadEntity? {
        val newDownload = DownloadEntity(
            url = url,
            fileName = fileName,
            mimeType = mime,
            extension = ext,
            filePath = "",
            status = "waiting...",
            createdAt = System.currentTimeMillis()
        )
        val id = db.downloadDao().insert(newDownload)
        return db.downloadDao().getByID(id)
    }

    fun initialDownload(url: String, fileName: String,ext: String,mime: String){
        scope.launch(Dispatchers.IO) {
           insertDownload(url, fileName, ext, mime)?.let {
                startDownload(it.id)
            }
        }
    }

    private fun startDownload(id : Long) {
       /* if (activeDownloads.values.any { it.isActive } && !forceResume) {
            if (downloadQueue.any { it.url == url }) return
        }*/
        scope.launch(Dispatchers.IO) {
            val entity = db.downloadDao().getByIDLimit(id) ?: return@launch
            if (activeDownloads.size < MAX_CONCURRENT_DOWNLOADS) {
                val job = scope.launch(Dispatchers.IO) {
                    val worker = instance?.let { DownloadWorker(context, db, it) }
                    worker?.downloadFile(entity.id, entity.url, entity.fileName)
                    activeDownloads.remove(entity.id)
                    checkQueue()
                }
                activeDownloads[entity.id] = job
            } else {
                downloadQueue.add(entity)
            }
        }
    }

    private fun checkQueue() {
        if (downloadQueue.isNotEmpty() && activeDownloads.size < MAX_CONCURRENT_DOWNLOADS) {
            downloadQueue.poll()?.let {
                startDownload(id = it.id)
            }
        } else {
            checkIfShouldStopExternally()
        }
    }


    fun pauseDownload(id: Long): Boolean {
        val job = activeDownloads[id]
        if (job != null) {
            job.cancel()
            activeDownloads.remove(id)
            scope.launch {
                val item = db.downloadDao().getByID(id)
                item?.let { db.downloadDao().update(it.copy(status = "paused")) }
            }
            return true
        }
        return false
    }

    fun resumeDownload(id: Long) {
        scope.launch {
            val item = db.downloadDao().getByID(id)
            if (item != null && item.status == "paused") {
                db.downloadDao().update(item.copy(status = "downloading"))
                startDownload(item.id)
            }
        }
    }

    fun deleteDownload(id: Long) {
        scope.launch(Dispatchers.IO) {
             db.downloadDao().getByID(id)?.let {
                val wasActive = pauseDownload(it.id)

                if (wasActive) {
                    activeDownloads.remove(it.id)
                }

                val wasInQueue = downloadQueue.removeIf { it1 -> it1.id == it.id }

                val file = File(it.filePath)
                if (file.exists()) file.delete()

                db.downloadDao().delete(it)

                if (!wasActive && wasInQueue && activeDownloads.size < MAX_CONCURRENT_DOWNLOADS) {
                    checkQueue()
                }
            }
        }
    }

    fun isTopDownload(id: Long): Boolean {
        return activeDownloads.keys.firstOrNull() == id
    }

    fun isIdle(): Boolean {
        return activeDownloads.isEmpty() && downloadQueue.isEmpty()
    }

    private fun checkIfShouldStopExternally() {
        scope.launch {
            val all = db.downloadDao().getAll()
            val onlyPausedOrCompleted = all.all { it.status == "paused" || it.status == "completed" }

            if (onlyPausedOrCompleted && activeDownloads.isEmpty() && downloadQueue.isEmpty()) {
                onAllDownloadsFinished?.invoke()
            }
        }
    }

}
/*if (!forceResume) {
               if (activeDownloads.containsKey(newId) || downloadQueue.any { it.first == url }) {
                   Log.d("DownloadManager", "Download already in progress or queued: $url")
                   return@launch
               }
           }*/
