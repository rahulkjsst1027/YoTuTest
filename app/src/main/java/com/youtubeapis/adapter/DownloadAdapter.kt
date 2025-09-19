package com.youtubeapis.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.youtubeapis.R
import com.youtubeapis.databinding.ItemDownloadBinding
import com.youtubeapis.model.DownloadEntity
import com.youtubeapis.utils.DownloadAction


class DownloadAdapter(var onToggleClick: (DownloadEntity, da: DownloadAction) -> Unit) :
    ListAdapter<DownloadEntity, DownloadAdapter.ViewHolder>(DIFF) {
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<DownloadEntity>() {
            override fun areItemsTheSame(old: DownloadEntity, new: DownloadEntity) =
                old.id == new.id

            override fun areContentsTheSame(old: DownloadEntity, new: DownloadEntity): Boolean {
                return  old.url == new.url &&
                        old.downloadedBytes == new.downloadedBytes &&
                        old.status == new.status &&
                        old.speed == new.speed &&
                        old.filePath == new.filePath &&
                        old.fileName == new.fileName &&
                        old.eta == new.eta
            }
        }
    }


    inner class ViewHolder(val binding: ItemDownloadBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val b = holder.binding

        b.fileName.text = item.fileName
       // b.status.text = item.status

        val progress =
            if (item.totalBytes == 0L) 0 else (item.downloadedBytes * 100 / item.totalBytes).toInt()
        b.progressBar.progress = progress
        val downloadedMB = bytesToMB(item.downloadedBytes)
        val totalMB = bytesToMB(item.totalBytes)
        b.percent.text = "$downloadedMB/$totalMB"

        val speedText = formatSpeed(item.speed)
        /*val speedText = formatSpeedDetailed(
            currentBytesPerSec = item.downloadedBytes,
            totalDownloadedBytes = item.totalBytes,
            elapsedMillis = System.currentTimeMillis() - item.updatedAt
        )*/

        b.speed.text = speedText


        b.timeRemaining.text = if (item.eta > 0L) {
             formatEta(item.eta)
        } else{
            item.status
        }

        when (item.status) {
            "paused" -> {
                b.playPause.visibility = View.VISIBLE
                b.progressBar.visibility = View.VISIBLE
                b.timeRemaining.visibility = View.VISIBLE
                b.playPause.setImageResource(R.drawable.download)
            }
            "downloading" -> {
                b.playPause.visibility = View.VISIBLE
                b.progressBar.visibility = View.VISIBLE
                b.timeRemaining.visibility = View.VISIBLE
                b.playPause.setImageResource(R.drawable.pause_button)
            }
            "failed" -> {
                b.playPause.visibility = View.VISIBLE
                b.progressBar.visibility = View.VISIBLE
                b.timeRemaining.visibility = View.VISIBLE
                b.playPause.setImageResource(R.drawable.pause_button)
            }
            else -> {
                b.playPause.visibility = View.GONE
                b.progressBar.visibility = View.GONE
                b.timeRemaining.visibility = View.GONE
            }
        }

        b.play.setOnClickListener {
            onToggleClick(item, DownloadAction.Clicked)
        }

        // Set toggle button text
       // b.btnToggle.text = if (item.status == "paused") "Resume" else "Pause"


        b.root.setOnClickListener {
            when (item.status) {
                "paused" -> {
                    onToggleClick(item, DownloadAction.Resume)
                }
                "downloading" -> {
                    onToggleClick(item, DownloadAction.Pause)
                }
                else -> {
                    onToggleClick(item, DownloadAction.Clicked)
                }
            }
        }

    }
    fun bytesToMB(bytes: Long): String {
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format("%.2f MB", mb)
    }
    fun formatEta(seconds: Long): String {
        val s = seconds % 60
        val m = (seconds / 60) % 60
        val h = (seconds / 3600) % 24
        val d = (seconds / (3600 * 24)) % 365
        val y = seconds / (3600 * 24 * 365)

        val parts = mutableListOf<String>()
        if (y > 0) parts.add("$y yr")
        if (d > 0) parts.add("$d d")
        if (h > 0) parts.add("$h hr")
        if (m > 0) parts.add("$m min")
        if (s > 0 || parts.isEmpty()) parts.add("$s sec")

        return parts.joinToString(" ")
    }
    private fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec == 0L) return ""
        val kb = bytesPerSec / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> String.format("%.2f GB/s", gb)
            mb >= 1 -> String.format("%.2f MB/s", mb)
            kb >= 1 -> String.format("%.2f KB/s", kb)
            else -> "$bytesPerSec B/s"
        }
    }
    fun formatSpeedDetailed(currentBytesPerSec: Long, totalDownloadedBytes: Long, elapsedMillis: Long): String {
        val current = formatSpeed(currentBytesPerSec)

        val averageBytesPerSec = if (elapsedMillis > 0)
            totalDownloadedBytes * 1000 / elapsedMillis
        else 0

        val average = formatSpeed(averageBytesPerSec)

        return "$current / $average"
    }




}
