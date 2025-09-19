package com.youtubeapis.worker

import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.youtubeapis.dio.AppDatabase
import com.youtubeapis.model.DownloadEntity
import com.youtubeapis.utils.DownloadUtils
import com.youtubeapis.utils.DownloadUtils.getDownloadFile
import com.youtubeapis.utils.NotificationUtils.showDownloadNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLConnection
import java.util.concurrent.ConcurrentHashMap

class DownloadWorker(
    private val context: Context,
    private val db: AppDatabase,
    private val instance: DownloadManager
) {
    private val client = OkHttpClient()
    private var lastUpdateTime = System.currentTimeMillis()
    private var lastProgress = 0


    suspend fun downloadFile(url: String, fileName: String) {
        val file = getDownloadFile(context, fileName)
        val downloadedBytes = if (file.exists()) file.length() else 0L

        val request = Request.Builder()
            .url(url)
            .addHeader("Range", "bytes=$downloadedBytes-")
            .build()

        val response = client.newCall(request).execute()

        // ✅ Check if server supports resume
        if (!response.isSuccessful || (response.code != 200 && response.code != 206)) {
            Log.e("DownloadWorker", "Server doesn't support resuming. Response code: ${response.code}")
            file.delete()
            return
        }

        val body = response.body ?: return
        val totalSize = downloadedBytes + body.contentLength()
        val input = body.byteStream()

        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int = 0
        var current = downloadedBytes
        val startTime = System.currentTimeMillis()

        try {
            withContext(Dispatchers.IO) {
                FileOutputStream(file, true).buffered().use { output ->
                    while (isActive && input.read(buffer).also { bytesRead = it } != -1) {
                        current += bytesRead
                        output.write(buffer, 0, bytesRead)

                        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                        val speed = if (elapsed > 0) (current - downloadedBytes) / elapsed else 0.0
                        val remaining = if (speed > 0) ((totalSize - current) / speed).toLong() else 0L

                        val progress = ((current * 100) / totalSize).toInt()
                        val now = System.currentTimeMillis()

                        if (progress != lastProgress || now - lastUpdateTime >= 1000) {
                            lastProgress = progress
                            lastUpdateTime = now


                            db.downloadDao().update(
                                DownloadEntity(
                                    url = url,
                                    fileName = fileName,
                                    filePath = file.absolutePath,
                                    status = "downloading",
                                    totalBytes = totalSize,
                                    downloadedBytes = current,
                                    speed = speed.toLong(),
                                    eta = remaining,
                                )
                            )

                        }
                    }
                }
            }
        } finally {
            withContext(Dispatchers.IO) {
                input.close()
            }
        }

        val mimeType = URLConnection.guessContentTypeFromName(fileName) ?: "application/octet-stream"
       /* val extension = fileName.substringAfterLast('.', "")
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension.lowercase()) ?: "application/octet-stream"*/

        // ✅ Mark as completed
        db.downloadDao().update(
            DownloadEntity(
                url = url,
                fileName = fileName,
                filePath = file.absolutePath,
                status = "completed",
                totalBytes = totalSize,
                downloadedBytes = totalSize,
                mimeType = mimeType
            )
        )

    }

    private suspend fun waitForInternet(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isConnected: () -> Boolean = {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            networkCapabilities != null &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        while (!isConnected()) {
            delay(1000) // Wait 1 second and check again
        }
    }


    suspend fun downloadFile(id: Long, url: String, fileName: String) {
        val file = getDownloadFile(context, fileName)
        val downloadedBytes = if (file.exists()) file.length() else 0L

        val request = Request.Builder()
            .url(url)
            .addHeader("Range", "bytes=$downloadedBytes-")
            .build()

        val response: Response
        try {
            response = client.newCall(request).execute()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "❌ Network error: ${e.message}")
            markAsFailed(id, url, fileName, file.absolutePath)
            return
        }

        if (!response.isSuccessful || (response.code != 200 && response.code != 206)) {
            Log.e("DownloadWorker", "❌ Server error: ${response.code}")
            file.delete()
            markAsFailed(id, url, fileName, file.absolutePath)
            return
        }

        val body = response.body ?: run {
            Log.e("DownloadWorker", "❌ Empty response body")
            markAsFailed(id, url, fileName, file.absolutePath)
            return
        }

        val totalSize = downloadedBytes + body.contentLength()
        val input = body.byteStream()

        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int = 0
        var current = downloadedBytes
        val startTime = System.currentTimeMillis()

        try {
            withContext(Dispatchers.IO) {
                FileOutputStream(file, true).buffered().use { output ->
                    while (isActive && input.read(buffer).also { bytesRead = it } != -1) {

                        // 🔌 Wait if offline
                        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                        val activeNetwork = connectivityManager.activeNetwork
                        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                        val hasInternet = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true


                        if (!hasInternet) {
                            Log.d("DownloadWorker", "🌐 No internet. Waiting to resume...")

                            // Update status to show paused state
                            db.downloadDao().getByIDLimit(id)?.let {
                                val paused = it.copy(
                                    status = "waiting",
                                    updatedAt = System.currentTimeMillis()
                                )
                                db.downloadDao().update(paused)
                            }

                            waitForInternet(context) // suspend until internet is back

                            // Mark status back to "downloading" once internet is back
                            db.downloadDao().getByIDLimit(id)?.let {
                                val resumed = it.copy(
                                    status = "downloading",
                                    updatedAt = System.currentTimeMillis()
                                )
                                db.downloadDao().update(resumed)
                            }

                            Log.d("DownloadWorker", "✅ Internet back. Resuming download.")
                            continue
                        }

                        current += bytesRead
                        output.write(buffer, 0, bytesRead)

                        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                        val speed = if (elapsed > 0) (current - downloadedBytes) / elapsed else 0.0
                        val remaining = if (speed > 0) ((totalSize - current) / speed).toLong() else 0L

                        val progress = ((current * 100) / totalSize).toInt()
                        val now = System.currentTimeMillis()

                        if (progress != lastProgress || now - lastUpdateTime >= 1000) {
                            lastProgress = progress
                            lastUpdateTime = now

                           db.downloadDao().getByIDLimit(id)?.let {
                                val updated = it.copy(
                                    filePath = file.absolutePath,
                                    status = "downloading",
                                    totalBytes = totalSize,
                                    downloadedBytes = current,
                                    speed = speed.toLong(),
                                    eta = remaining,
                                    updatedAt = System.currentTimeMillis()
                                )
                                db.downloadDao().update(updated)
                            }


                            if (instance.isTopDownload(id)) {
                                if (progress < 100) {
                                    showDownloadNotification(
                                        id.toInt(),
                                        context,
                                        fileName,
                                        progress,
                                        speed.toLong(),
                                        remaining
                                    )
                                } else {
                                    // ✅ Remove notification once completed
                                    val notificationManager =
                                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                    notificationManager.cancel(id.toInt())
                                }
                            }

                        }
                    }
                }
            }
        }
        catch (e: Exception) {
            Log.e("DownloadWorker", "❌ Download error: ${e.message}")
            markAsFailed(id, url, fileName, file.absolutePath)
            return
        } finally {
            withContext(Dispatchers.IO) {
                input.close()
            }
        }

        val existing = db.downloadDao().getByIDLimit(id)
        if (existing != null) {
            // First: Mark as "completed" in private folder
            val interim = existing.copy(
                filePath = file.absolutePath,
                status = "completed",
                totalBytes = totalSize,
                downloadedBytes = current,
                speed = 0L,
                eta = -1L,
                updatedAt = System.currentTimeMillis()
            )
            db.downloadDao().update(interim)

            // Then: Move to public folder and update path
            DownloadUtils.moveFileToPublicDownloads(context, file, fileName, interim.mimeType!!, interim.extension!!) { movedUri ->
                if (movedUri != null) {
                    val final = interim.copy(
                        filePath = movedUri.toString(),
                        updatedAt = System.currentTimeMillis()
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        db.downloadDao().update(final)
                    }
                }
            }
        }


    }

    private suspend fun markAsFailed(id: Long, url: String, fileName: String, filePath: String, string: String?  = null) {
        db.downloadDao().update(
            DownloadEntity(
                id = id,
                url = url,
                fileName = fileName,
                filePath = filePath,
                status = string ?: "failed",
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun downloadFileMultiThreaded(id: Long, url: String, fileName: String, threadCount: Int = 4) {
        val client = OkHttpClient()
        val mainFile = getDownloadFile(context, fileName)
        val tempDir = File(context.cacheDir, "parts_$id").apply { mkdirs() }

        // Step 1: Get total file size
        val sizeRequest = Request.Builder().url(url).head().build()
        val sizeResponse = client.newCall(sizeRequest).execute()
        val totalSize = sizeResponse.body?.contentLength() ?: -1L
        sizeResponse.close()

        if (totalSize <= 0L) {
            Log.e("MultiDownload", "❌ Invalid file size")
            markAsFailed(id, url, fileName, mainFile.absolutePath)
            return
        }

        // Step 2: Divide ranges
        val partSize = totalSize / threadCount
        val ranges = List(threadCount) { i ->
            val start = i * partSize
            val end = if (i == threadCount - 1) totalSize - 1 else (start + partSize - 1)
            start to end
        }

        val speedMap = ConcurrentHashMap<Int, Long>()
        val downloadedMap = ConcurrentHashMap<Int, Long>()

        // Step 3: Launch download threads
        coroutineScope {
            ranges.mapIndexed { index, (start, end) ->
                launch(Dispatchers.IO) {
                    val partFile = File(tempDir, "part_$index.tmp")
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Range", "bytes=$start-$end")
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) throw IOException("Thread $index failed")

                    val input = response.body?.byteStream() ?: return@launch
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var current = 0L
                    val startTime = System.currentTimeMillis()

                    FileOutputStream(partFile).use { output ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            current += bytesRead
                            downloadedMap[index] = current
                            val elapsed = System.currentTimeMillis() - startTime
                            if (elapsed > 0) speedMap[index] = (current * 1000 / elapsed)
                        }
                    }

                    input.close()
                }
            }.joinAll()
        }

        // Step 4: Merge parts
        withContext(Dispatchers.IO) {
            FileOutputStream(mainFile).use { output ->
                for (i in 0 until threadCount) {
                    val partFile = File(tempDir, "part_$i.tmp")
                    FileInputStream(partFile).use { input ->
                        input.copyTo(output)
                    }
                    partFile.delete()
                }
            }
            tempDir.deleteRecursively()
        }

        // Step 5: Report final info
        val totalDownloaded = downloadedMap.values.sum()
        val avgSpeed = speedMap.values.average().toLong()

        db.downloadDao().update(
            DownloadEntity(
                id = id,
                url = url,
                fileName = fileName,
                filePath = mainFile.absolutePath,
                status = "completed",
                totalBytes = totalSize,
                downloadedBytes = totalDownloaded,
                speed = avgSpeed,
                eta = 0L,
            )
        )
    }


}
/*val combinedSpeed = speedMap.values.sum()
val totalDownloaded = downloadedMap.values.sum()
val elapsed = System.currentTimeMillis() - startTime

val averageSpeed = if (elapsed > 0) (totalDownloaded * 1000 / elapsed) else 0

val speedText = "${formatSpeed(combinedSpeed)} / ${formatSpeed(averageSpeed)}"*/


/*
suspend fun downloadFile(url: String, fileName: String) {
    val file = getDownloadFile(context, fileName)
    val downloadedBytes = if (file.exists()) file.length() else 0L

    val request = Request.Builder()
        .url(url)
        .addHeader("Range", "bytes=$downloadedBytes-")
        .build()

    val response = client.newCall(request).execute()

    // ✅ Check if server supports resume
    if (!response.isSuccessful || (response.code != 200 && response.code != 206)) {
        Log.e("DownloadWorker", "Server doesn't support resuming. Response code: ${response.code}")
        file.delete()
        return
    }

    val body = response.body ?: return
    val totalSize = downloadedBytes + body.contentLength()
    val input = body.byteStream()

    val buffer = ByteArray(8 * 1024)
    var bytesRead: Int = 0
    var current = downloadedBytes
    val startTime = System.currentTimeMillis()

    try {
        withContext(Dispatchers.IO) {
            FileOutputStream(file, true).buffered().use { output ->
                while (isActive && input.read(buffer).also { bytesRead = it } != -1) {
                    current += bytesRead
                    output.write(buffer, 0, bytesRead)

                    val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                    val speed = if (elapsed > 0) (current - downloadedBytes) / elapsed else 0.0
                    val remaining = if (speed > 0) ((totalSize - current) / speed).toLong() else 0L

                    val progress = ((current * 100) / totalSize).toInt()
                    val now = System.currentTimeMillis()

                    if (progress != lastProgress || now - lastUpdateTime >= 1000) {
                        lastProgress = progress
                        lastUpdateTime = now

                        db.downloadDao().insert(
                            DownloadEntity(
                                url, fileName, file.absolutePath,
                                totalSize, current, "downloading",
                                speed.toLong(), remaining
                            )
                        )

                        if (instance.isTopDownload(url)) {
                            showDownloadNotification(
                                context,
                                fileName,
                                progress,
                                speed.toLong(),
                                remaining
                            )
                        }
                    }
                }
            }
        }
    } finally {
        withContext(Dispatchers.IO) {
            input.close()
        }
    }

    // ✅ Mark as completed
    db.downloadDao().insert(
        DownloadEntity(url, fileName, file.absolutePath, totalSize, totalSize, "completed", 0L, 0L)
    )
}
*/

