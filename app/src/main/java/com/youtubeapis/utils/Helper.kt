package com.youtubeapis.utils

object Helper {

    fun formatSpeed(bytesPerSec: Long): String {
        val kb = bytesPerSec / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1 -> String.format("%.2f MB/s", mb)
            kb >= 1 -> String.format("%.1f KB/s", kb)
            else -> "$bytesPerSec B/s"
        }
    }

    fun formatTimeRemaining(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return if (minutes > 0) "${minutes}m ${secs}s left" else "${secs}s left"
    }

}