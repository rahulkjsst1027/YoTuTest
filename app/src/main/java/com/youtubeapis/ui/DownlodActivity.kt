package com.youtubeapis.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.youtubeapis.MyApp
import com.youtubeapis.R
import com.youtubeapis.adapter.DownloadAdapter
import com.youtubeapis.databinding.ActivityDownlodBinding
import com.youtubeapis.dio.AppDatabase
import com.youtubeapis.service.DownloadService
import com.youtubeapis.utils.DownloadAction
import com.youtubeapis.utils.DownloadUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownlodActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownlodBinding
    private lateinit var downloadAdapter: DownloadAdapter
    private lateinit var downloadAdapter2: DownloadAdapter

    private lateinit var db: AppDatabase
    private lateinit var scope: CoroutineScope
    private var downloadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDownlodBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val app = applicationContext as MyApp
        db = app.database
        scope = app.appScope

        downloadAdapter = createDownloadAdapter()
        binding.recyclerView.adapter = downloadAdapter

        downloadAdapter2 = createDownloadAdapter()
        binding.recyclerView2.adapter = downloadAdapter2

        observeDownloads()
    }

    private fun createDownloadAdapter(): DownloadAdapter {
        return DownloadAdapter { entity, da ->
            when (da) {
                DownloadAction.Clicked -> {
                    if (entity.status == "completed"){
                        val file = DownloadUtils.uriToFile(this, Uri.parse(entity.filePath)) ?: return@DownloadAdapter
                        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
                        startActivity(Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "video/mp4")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    }else{

                    }
                }

                DownloadAction.Resume, DownloadAction.Pause -> {
                    val intent = Intent(this, DownloadService::class.java).apply {
                        action = if (da == DownloadAction.Resume) "ACTION_RESUME" else "ACTION_PAUSE"
                        putExtra("id", entity.id.toString())
                        putExtra("fileName", entity.status)
                    }
                    startService(intent)
                }
            }
        }
    }


    private fun observeDownloads() {
        downloadJob?.cancel()
        downloadJob = scope.launch {
            launch {
                db.downloadDao().getAllFlow("completed").collect { items ->
                    withContext(Dispatchers.Main) {
                        downloadAdapter.submitList(items)
                    }
                }
            }
            launch {
                db.downloadDao().getAllFlow2("completed").collect { items ->
                    withContext(Dispatchers.Main) {
                        downloadAdapter2.submitList(items)
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        downloadJob?.cancel()
        checkIfShouldStopExternally()
    }

    private fun checkIfShouldStopExternally() {
        scope.launch {
            val all = db.downloadDao().getAll()
            val onlyPausedOrCompleted = all.all { it.status == "paused" || it.status == "completed" }
            if (onlyPausedOrCompleted) {
                val intent = Intent(applicationContext, DownloadService::class.java)
                stopService(intent)
            }
        }
    }



}


//  val downloadManager = DownloadManager.getInstance(applicationContext, db, scope)
//  downloadManager.checkIfShouldStopExternally()
/*
fun startDownloadService(context: Context, url: String, fileName: String) {
    val intent = Intent(context, DownloadService::class.java).apply {
        putExtra("url", url)
        putExtra("fileName", fileName)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}*/
