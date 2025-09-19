package com.youtubeapis.files

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

object FileClickHandler {

    private var showHiddenFiles = false

    val rootPath = "/storage/emulated/0"

    fun listFiles1(path: String): List<File> {
        val file = File(path)
        val files = file.listFiles { f ->
            // Agar hidden file show karni hain → sab allow karo
            // Agar nahi dikhani → jo "." se start hoti hain unko skip karo
            showHiddenFiles || !f.name.startsWith(".")
        } ?: emptyArray()
        files.sorted().forEach {
            Log.i("TAG", "listFiles: ${it.absolutePath}")
        }

        return files.sortedWith(compareBy({ !it.isDirectory }, { it.name })).toList()
    }

    suspend fun listFiles2(path: String): List<File> = withContext(Dispatchers.IO) {
        val file = File(path)
        val files = file.listFiles()?.filter { showHiddenFiles || !it.name.startsWith(".") } ?: emptyList()
        files // no sorting yet
    }

    suspend fun listFiles(path: String): List<File> {
        var result: List<File>
        val timeTaken = measureTimeMillis {
            result = withContext(Dispatchers.IO) {
                val file = File(path)
                val filesSequence = file.listFiles()?.asSequence() ?: emptySequence()
                val filtered = filesSequence.filter { showHiddenFiles || !it.name.startsWith(".") }
                filtered.toList()
            }
        }
        println("listFiles executed in $timeTaken ms")
        return result
    }




    suspend fun createVideoThumbnail(file: File): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29+
                ThumbnailUtils.createVideoThumbnail(file, Size(200, 200), null)
            } else {
                // Pre Q
                ThumbnailUtils.createVideoThumbnail(
                    file.absolutePath,
                    MediaStore.Images.Thumbnails.MINI_KIND
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun getBreadcrumb(path: String, rootPath: String): SpannableStringBuilder {
        val root = if (path.startsWith(rootPath)) "⌂ Internal Storage" else ""
        val subPaths = path.removePrefix(rootPath)
            .split("/")
            .filter { it.isNotEmpty() }

        val allParts = listOf(root) + subPaths
        val breadcrumb = SpannableStringBuilder()

        val separator = " ➤ " // Emoji separator

        allParts.forEachIndexed { index, part ->
            val start = breadcrumb.length
            breadcrumb.append(part)
            val end = breadcrumb.length

            // Last part → highlight (blue/green)
            if (index == allParts.size - 1) {
                breadcrumb.setSpan(
                    ForegroundColorSpan(Color.GREEN),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // Add separator
            if (index != allParts.size - 1) {
                breadcrumb.append(separator)
            }
        }

        return breadcrumb
    }

    fun getFileType(file: File): String {
        return when {
            file.isDirectory -> "Folder"
            isImage(file) -> "Image"
            isVideo(file) -> "Video"
            isAudio(file) -> "Audio"
            isPdf(file) -> "PDF"
            isDoc(file) -> "Document"
            isApk(file) -> "APK"
            isZip(file) -> "ZIP"
            else -> "File"
        }
    }


    @SuppressLint("ObsoleteSdkInt")
    fun openFile(file: File, mimeType: String, context: Context) {
        try {
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    context.packageName + ".provider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Cannot open this file", Toast.LENGTH_SHORT).show()
        }
    }

    fun installApk(file: File, context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canInstall = context.packageManager.canRequestPackageInstalls()
                if (!canInstall) {
                    // Ask user to allow install unknown apps
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:" + context.packageName))
                    context.startActivity(intent)
                    Toast.makeText(context, "Allow install unknown apps first", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    context.packageName + ".provider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Cannot install APK", Toast.LENGTH_SHORT).show()
        }
    }

    // ===== File type checks =====
    fun isImage(file: File) = file.extension.lowercase() in listOf("jpg","jpeg","png","webp","gif")
    fun isVideo(file: File) = file.extension.lowercase() in listOf("mp4","mkv","avi","mov","flv","3gp","webm","m3u8")
    fun isAudio(file: File) = file.extension.lowercase() in listOf("mp3","wav","m4a","aac","flac","ogg")
    fun isApk(file: File) = file.extension.lowercase() == "apk"
    fun isPdf(file: File) = file.extension.lowercase() == "pdf"
    fun isDoc(file: File) = file.extension.lowercase() in listOf("doc","docx","txt")
    fun isZip(file: File) = file.extension.lowercase() in listOf("zip","rar","7z")
}
