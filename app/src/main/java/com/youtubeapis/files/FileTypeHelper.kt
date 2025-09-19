package com.youtubeapis.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import com.youtubeapis.R
import java.io.File

object FileTypeHelper {

    fun getFileIcon(file: File, context: Context): Drawable {
        return try {
            when {
               /* file.isDirectory -> ContextCompat.getDrawable(context, R.drawable.folder)!!
                isImage(file) -> ContextCompat.getDrawable(context, R.drawable.picture)!!
                isVideo(file) -> {
                    val thumb = ThumbnailUtils.createVideoThumbnail(
                        file.absolutePath,
                        MediaStore.Images.Thumbnails.MINI_KIND
                    )
                    thumb?.let { BitmapDrawable(context.resources, it) }
                        ?: ContextCompat.getDrawable(context, R.drawable.video)!!
                }*/
                isAudio(file) -> ContextCompat.getDrawable(context, R.drawable.audio)!!
                isApk(file) -> getApkIcon(file, context) ?: ContextCompat.getDrawable(context, R.drawable.file)!!
                isPdf(file) -> ContextCompat.getDrawable(context, R.drawable.pdf)!!
                isDoc(file) -> ContextCompat.getDrawable(context, R.drawable.docs)!!
                isZip(file) -> ContextCompat.getDrawable(context, R.drawable.zip)!!
                else -> ContextCompat.getDrawable(context, R.drawable.other)!!
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ContextCompat.getDrawable(context, R.drawable.other)!!
        }
    }

    fun isImage(file: File) = file.extension.lowercase() in listOf("jpg","jpeg","png","webp","gif")
    fun isVideo(file: File) = file.extension.lowercase() in listOf(
        "mp4", "mkv", "avi", "mov", "flv", "3gp", "webm", "m3u8"
    )

    fun isAudio(file: File) = file.extension.lowercase() in listOf(
        "mp3", "wav", "m4a", "aac", "flac", "ogg"
    )
    private fun isApk(file: File) = file.extension.lowercase() == "apk"
    private fun isPdf(file: File) = file.extension.lowercase() == "pdf"
    private fun isDoc(file: File) = file.extension.lowercase() in listOf("doc","docx","txt")
    private fun isZip(file: File) = file.extension.lowercase() in listOf("zip","rar","7z")

    private fun getApkIcon(file: File, context: Context): Drawable? {
        return try {
            val pm = context.packageManager
            val packageInfo = pm.getPackageArchiveInfo(file.absolutePath, 0)
            packageInfo?.applicationInfo?.apply {
                sourceDir = file.absolutePath
                publicSourceDir = file.absolutePath
            }?.loadIcon(pm)
        } catch (e: Exception) {
            Log.e("FileTypeHelper", "APK icon load failed: ${e.message}")
            null
        }
    }

    fun getFolderBadge(file: File, context: Context): Drawable? {
        if (!file.isDirectory) return null

        // Folder name based check
        val name = file.name.lowercase()
        val badgeRes = when {
            name.contains("audio") -> R.drawable.audio
            name.contains("video") -> R.drawable.video
            name.contains("image") || name.contains("photo") -> R.drawable.picture
            else -> null
        }

        return badgeRes?.let { ContextCompat.getDrawable(context, it) }
    }


    fun loadFolderWithBadge(file: File, context: Context): Bitmap? {
        val folderDrawable = ContextCompat.getDrawable(context, R.drawable.folder)
        val badgeDrawable = getFolderBadge(file, context)

        if (folderDrawable !is BitmapDrawable) return null

        val folderBitmap = folderDrawable.bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(folderBitmap)

        if (badgeDrawable is BitmapDrawable) {
            val density = context.resources.displayMetrics.density

            // Badge background dimensions
            val backgroundWidth = (44 * density).toInt()    // width same
            val backgroundHeight = (44 * density).toInt()   // height kam
            val iconSize = (30 * density).toInt()           // icon size
            val margin = (4 * density).toFloat()            // margin from folder edge

            // Resize badge icon
            val badgeBitmap = Bitmap.createScaledBitmap(
                badgeDrawable.bitmap,
                iconSize,
                iconSize,
                true
            )

            // Position → bottom-right with margin
            val left = folderBitmap.width - backgroundWidth - margin
            val top = folderBitmap.height - backgroundHeight - margin

            // Draw rounded rectangle background
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.LTGRAY
            paint.style = Paint.Style.FILL
            val cornerRadius = 16 * density // rounded corners
            val rectF = RectF(left, top, left + backgroundWidth, top + backgroundHeight)
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)

            // Draw badge icon at center of background
            val iconLeft = left + (backgroundWidth - badgeBitmap.width) / 2f
            val iconTop = top + (backgroundHeight - badgeBitmap.height) / 2f
            canvas.drawBitmap(badgeBitmap, iconLeft, iconTop, null)
        }

        return folderBitmap
    }







}
