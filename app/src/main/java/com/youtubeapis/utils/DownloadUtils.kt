package com.youtubeapis.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DownloadUtils {

    private fun getSubFolderByExtension(ext: String): String {
        if (ext.isEmpty()) return "Others"
        val lowerExt = ext.lowercase()

        // MIME-based matching first
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(lowerExt)?.let { mimeType ->
            return when {
                mimeType.startsWith("image/") -> "Images"
                mimeType.startsWith("audio/") -> "Audio"
                mimeType.startsWith("video/") -> "Video"
                mimeType.startsWith("text/") -> "Docs"
                mimeType in setOf(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/rtf",
                    "application/json",
                    "application/xml"
                ) -> "Docs"
                mimeType == "application/vnd.android.package-archive" -> "Files"
                mimeType.startsWith("application/zip") ||
                        mimeType.contains("x-rar") ||
                        mimeType.contains("x-7z-compressed") -> "Archives"
                else -> "Others"
            }
        }

        // Extension-based fallback
        return when (lowerExt) {
            // Images
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg", "heic", "ico", "tiff", "psd", "raw" -> "Images"

            // Audio
            "mp3", "aac", "wav", "ogg", "flac", "m4a", "wma", "opus", "alac", "aiff", "mid" -> "Audio"

            // Video
            "mp4", "mkv", "avi", "mov", "flv", "wmv", "3gp", "mpeg", "webm", "m4v", "vob" -> "Video"

            // Documents
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf",
            "odt", "ods", "odp", "epub", "md", "tex", "log", "pages", "numbers", "key" -> "Docs"

            // Files (Apps/Installers/Scripts/Executables)
            "apk", "exe", "msi", "dmg", "pkg", "deb", "rpm", "jar", "bat", "sh", "cmd", "bin", "msix", "vb", "class" -> "Files"

            // Archives
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso", "cab", "z", "lz" -> "Archives"

            // Fonts (optional)
            "ttf", "otf", "woff", "woff2", "eot" -> "Fonts"

            // Code (optional group)
            "java", "kt", "js", "ts", "html", "css", "py", "c", "cpp", "cs", "php", "go", "swift", "rs", "json", "xml" -> "Code"

            // Others (fallback)
            else -> "Others"
        }
    }



    fun getDownloadFile(context: Context, fileName: String): File {
        val ext = fileName.substringAfterLast('.', "")
        val folder = File(context.getExternalFilesDir(null), "KM/${getSubFolderByExtension(ext)}")
        if (!folder.exists()) folder.mkdirs()
        return File(folder, fileName)
    }


    fun moveFileToPublicDownloads(
        context: Context,
        sourceFile: File,
        fileName: String,
        mimeType: String,
        ext: String,
        callBack: (uri: Uri?) -> Unit
    ) {
       val subFolder = getSubFolderByExtension(ext)

        val resolver = context.contentResolver

       // return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // ✅ Android 10+ → Use MediaStore with RELATIVE_PATH
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/KM/$subFolder")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return callBack.invoke(null)

            try {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }

                // ✅ Mark as finished
                val completedValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(uri, completedValues, null, null)

                // Delete original
                sourceFile.delete()

                return callBack.invoke(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                callBack.invoke(null)
            }

        } else {
            // ✅ Android 9 and below → Use public directory + MediaScanner
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "KM/$subFolder").apply { mkdirs() }
                val targetFile = File(targetDir, fileName)

                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Media scanner to make file visible
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )

                sourceFile.delete()
                return callBack.invoke(Uri.fromFile(targetFile))

            } catch (e: Exception) {
                e.printStackTrace()
                callBack.invoke(null)
            }
        }
    }



    fun writeToPublicFolder(context: Context, fileName: String, data: ByteArray): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val subFolder = getSubFolderByExtension(ext)

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // ✅ Android 10+ (API 29+) → Use MediaStore
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "KM/$subFolder")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return false

                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(data)
                    outputStream.flush()
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

            } else {
                // ✅ Android 9 and below → Direct external storage write
                val dir = File(Environment.getExternalStorageDirectory(), "KM/$subFolder")
                if (!dir.exists()) dir.mkdirs()

                val file = File(dir, fileName)
                FileOutputStream(file).use { output ->
                    output.write(data)
                    output.flush()
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    fun getUriFromPath(context: Context, path: String): Uri? {
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
        val selectionArgs = arrayOf(path)

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idColumn)
                return ContentUris.withAppendedId(uri, id)
            }
        }
        return null
    }

    fun uriToFile(context: Context, uri: Uri): File? {
        return when (uri.scheme) {
            "file" -> File(uri.path ?: return null)
            "content" -> {
                // Query the file path from MediaStore (only valid for some content Uris, below API 29)
                val projection = arrayOf(MediaStore.MediaColumns.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                    if (cursor.moveToFirst()) {
                        val filePath = cursor.getString(columnIndex)
                        if (!filePath.isNullOrEmpty()) return File(filePath)
                    }
                }

                // Fallback: copy to cache and return that
                copyUriToCache(context, uri)
            }
            else -> null
        }
    }

    fun copyUriToCache(context: Context, uri: Uri): File? {
        return try {
            val fileName = getFileNameFromUri(context, uri) ?: "temp_file"
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)

            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else null
        }
    }


}