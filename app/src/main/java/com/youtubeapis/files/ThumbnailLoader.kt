package com.youtubeapis.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

object ThumbnailLoader {
    private val videoCache = mutableMapOf<String, Bitmap>()
    private val imageCache = mutableMapOf<String, Bitmap>()

    fun loadVideoThumbnail(file: File, target: ImageView, activity: AppCompatActivity) {
        val key = file.absolutePath
        videoCache[key]?.let {
            target.scaleType = ImageView.ScaleType.CENTER_CROP
            target.setImageBitmap(it)
            return
        }

        activity.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ThumbnailUtils.createVideoThumbnail(file, Size(200, 200), null)
                    } else {
                        ThumbnailUtils.createVideoThumbnail(file.path, MediaStore.Images.Thumbnails.MINI_KIND)
                    }
                } catch (e: Exception) { null }
            }
            bitmap?.let {
                val cropped = centerCropBitmap(it, target.width, target.height)
                videoCache[key] = cropped
                target.scaleType = ImageView.ScaleType.CENTER_CROP
                target.setImageBitmap(cropped)
            }
        }
    }

    fun loadImage(file: File, target: ImageView, activity: AppCompatActivity) {
        val key = file.absolutePath
        imageCache[key]?.let {
            target.scaleType = ImageView.ScaleType.CENTER_CROP
            target.setImageBitmap(it)
            return
        }

        activity.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)
                } catch (e: Exception) { null }
            }
            bitmap?.let {
                val cropped = centerCropBitmap(it, target.width, target.height)
                imageCache[key] = cropped
                target.scaleType = ImageView.ScaleType.CENTER_CROP
                target.setImageBitmap(cropped)
            }
        }
    }

    private fun centerCropBitmap(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val srcWidth = source.width
        val srcHeight = source.height
        val scale = max(targetWidth.toFloat() / srcWidth, targetHeight.toFloat() / srcHeight)
        val scaledWidth = (scale * srcWidth).toInt()
        val scaledHeight = (scale * srcHeight).toInt()
        val left = (scaledWidth - targetWidth) / 2
        val top = (scaledHeight - targetHeight) / 2
        val scaledBitmap = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        return Bitmap.createBitmap(scaledBitmap, left, top, targetWidth, targetHeight)
    }
}
