package com.youtubeapis.files

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.youtubeapis.R
import com.youtubeapis.databinding.ActivityFileBinding
import com.youtubeapis.files.FileClickHandler.getBreadcrumb
import com.youtubeapis.files.FileClickHandler.installApk
import com.youtubeapis.files.FileClickHandler.isApk
import com.youtubeapis.files.FileClickHandler.isAudio
import com.youtubeapis.files.FileClickHandler.isDoc
import com.youtubeapis.files.FileClickHandler.isImage
import com.youtubeapis.files.FileClickHandler.isPdf
import com.youtubeapis.files.FileClickHandler.isVideo
import com.youtubeapis.files.FileClickHandler.isZip
import com.youtubeapis.files.FileClickHandler.listFiles
import com.youtubeapis.files.FileClickHandler.openFile
import com.youtubeapis.files.FileClickHandler.rootPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFileBinding
    private lateinit var adapter: FileAdapter


    private var currentDir = rootPath

    private fun handleFileClick(file: File, context: Context) {
        when {
            file.isDirectory -> {
                currentDir = file.absolutePath
                updateFiles()
            }
            isImage(file) -> {
               /* val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                Log.i("TAG", "handleFileClick: $uri")
                val intent = Intent(context, ImagePreviewActivity::class.java)
                intent.data = uri
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                fileActionLauncher.launch(intent)*/

                val intent = Intent(context, ImagePreviewActivity::class.java)
                intent.putExtra("data", file.absolutePath)
                fileActionLauncher.launch(intent)

            }
            isVideo(file) -> {
                /* val uri = FileProvider.getUriForFile(
                     context,
                     "${context.packageName}.provider",
                     file
                 )
                 Log.i("TAG", "handleFileClick: $uri")
                 val intent = Intent(context, ImagePreviewActivity::class.java)
                 intent.data = uri
                 intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                 fileActionLauncher.launch(intent)*/


                Log.i("TAG", "handleFileClick: ${file.absolutePath}")
                val intent = Intent(context, VideoPlayerActivity::class.java)
                // val intent = Intent(context, Player2Activity::class.java)
                intent.putExtra("data", file.absolutePath)
                fileActionLauncher.launch(intent)

            }
           // isVideo(file) -> openFile(file, "video/*", context)
            isAudio(file) -> openFile(file, "audio/*", context)
            isApk(file) -> installApk(file, context)
            isPdf(file) -> openFile(file, "application/pdf", context)
            isDoc(file) -> openFile(file, "application/msword", context)
            isZip(file) -> openFile(file, "application/zip", context)
            else -> openFile(file, "*/*", context)
        }
    }

    private val fileActionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        updateFiles()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        adapter = FileAdapter (
            onClick = { file -> handleFileClick(file, this) },
            onLongClick = { file ->
                adapter.isSelectionMode = true
                adapter.toggleSelection(file)
                adapter.notifyDataSetChanged()
                showSelectionUI()
            },
            onSelectionChange = { updateTopCheckBox() }
        )
        binding.fileRecyclerView.apply {
            setHasFixedSize(true) // agar item ka size fix hai (height/width)
           // itemAnimator = null   // unnecessary animations hatane ke liye
            setItemViewCacheSize(2) // jitne items cache karna chahte ho (by default 2–3)
            recycledViewPool.setMaxRecycledViews(0, 5) // ek type ke 5 items cache
        }
        binding.fileRecyclerView.isVerticalScrollBarEnabled = true
        ViewCompat.setScrollIndicators(binding.fileRecyclerView, ViewCompat.SCROLL_INDICATOR_RIGHT)
        binding.fileRecyclerView.adapter = adapter




        checkStoragePermission()



        binding.btnCancel.setOnClickListener {
            onBackPressed()
        }

        binding.btnCopy.setOnClickListener {
            val selected = adapter.getSelectedFiles()
            val dest = File("/storage/emulated/0/TargetFolder")
            startCopy(selected, dest)
        }

        binding.btnMove.setOnClickListener {
            val selected = adapter.getSelectedFiles()
            val dest = File("/storage/emulated/0/TargetFolder")
            startMove(selected, dest)
        }


        binding.fileCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (binding.fileCheckBox.isPressed) { // only user clicks
                if (isChecked) adapter.selectAll() else adapter.unselectAll()
                updateTopCheckBox()
            }
        }


        binding.sort.setOnClickListener {
            val popup = PopupMenu(this, binding.sort)
            popup.menuInflater.inflate(R.menu.sort_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sort_name -> sortAndOrder(SortOption.NAME)
                    R.id.sort_date -> sortAndOrder(SortOption.DATE)
                    R.id.sort_type -> sortAndOrder(SortOption.TYPE)
                    R.id.sort_size -> sortAndOrder(SortOption.SIZE)
                }
                true
            }
            popup.show()
        }
        binding.order.setOnClickListener {
            currentOrder = if (currentOrder == SortOrder.ASCENDING) {
                binding.order.setImageResource(R.drawable.baseline_arrow_down) // descending icon
                SortOrder.DESCENDING
            } else {
                binding.order.setImageResource(R.drawable.baseline_arrow_up) // ascending icon
                SortOrder.ASCENDING
            }

            // Re-sort current list with new order
            sortAndOrder(adapter.currentSort)
        }


    }
    private var currentOrder = SortOrder.ASCENDING
    private fun sortAndOrder(option: SortOption){
        adapter.setSortOption(option, currentOrder, binding.fileRecyclerView, binding.progressBar)
    }
    private fun updateTopCheckBox() {
        binding.fileCheckBox.setOnCheckedChangeListener(null)
        binding.fileCheckBox.isChecked = adapter.isAllSelected()
        binding.fileCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (binding.fileCheckBox.isPressed) { // only user clicks
                if (isChecked) adapter.selectAll() else adapter.unselectAll()
                updateTopCheckBox()
            }
        }

        val count = adapter.getSelectedCount()
        binding.title.text = "Selected: $count"
    }


    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                allFilesAccessLauncher.launch(intent)
            }
            updateFiles()
        } else {
            // Android 10 and below
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                storagePermissionLauncher.launch(permission)
            } else {
                updateFiles()
            }
        }
    }

    // Launcher for READ_EXTERNAL_STORAGE (Android 10 and below)
    private var storagePermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) updateFiles()
        else Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
    }

    // Launcher for MANAGE_EXTERNAL_STORAGE (Android 11+)
    private var allFilesAccessLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) updateFiles()
            else Toast.makeText(this, "All Files Access denied", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateFiles2() {

        binding.pathTextView.isSingleLine = true
        binding.pathTextView.ellipsize = TextUtils.TruncateAt.START
        binding.pathTextView.text = getBreadcrumb(currentDir, rootPath)

        lifecycleScope.launch {
            val allFiles = listFiles(currentDir)
            adapter.clearFiles()

            val chunkSize = 100
            var currentList = emptyList<File>()

            allFiles.chunked(chunkSize).forEach { chunk ->
                currentList = currentList + chunk // append chunk to old list
                adapter.updateFiles(currentList)  // update adapter
                delay(30) // UI ko smooth rakhe
            }

        }
    }


    private var loadJob: Job? = null
    private fun updateFiles() {
        binding.pathTextView.isSingleLine = true
        binding.pathTextView.ellipsize = TextUtils.TruncateAt.START
        binding.pathTextView.text = getBreadcrumb(currentDir, rootPath)
       // Cancel previous loading if any
        loadJob?.cancel()
        loadJob =  lifecycleScope.launch {
            // 👇 loader show
            binding.progressBar.visibility = View.VISIBLE
           // binding.fileRecyclerView.visibility = View.GONE

            val allFiles = listFiles(currentDir)
            adapter.clearFiles()

            val chunkSize = 100
            var currentList = emptyList<File>()

            allFiles.chunked(chunkSize).forEach { chunk ->
                // 1️⃣ Heavy work in background thread
                val processedChunk = withContext(Dispatchers.Default) {
                    chunk.map { it }
                }
// Cancel check → if Activity destroyed or job cancelled
                if (!isActive) return@launch
                // 2️⃣ Adapter update in Main thread
                withContext(Dispatchers.Main) {
                    currentList = currentList + processedChunk
                    adapter.updateFiles(currentList)
                }

                // 3️⃣ Optional minimal delay for UI smoothness
                delay(0) // ya 0, device ke hisaab se
            }

            // Cancel check before hiding loader
            if (isActive) {
                binding.progressBar.visibility = View.GONE
            }
            // 👇 loader hide
           // binding.progressBar.visibility = View.GONE
           // binding.fileRecyclerView.visibility = View.VISIBLE
        }
    }



    private fun showSelectionUI() {
        binding.btnCancel.setImageResource(R.drawable.baseline_cancel_24)
        binding.selectionBar.visibility = View.VISIBLE
        binding.fileCheckBox.visibility = View.VISIBLE
        binding.search.visibility = View.GONE
    }

    private fun hideSelectionUI() {
        binding.btnCancel.setImageResource(R.drawable.ic_back)
        binding.title.text = "Storage"
        binding.selectionBar.visibility = View.GONE
        binding.fileCheckBox.visibility = View.GONE
        binding.search.visibility = View.VISIBLE
        binding.fileCheckBox.isChecked = false  // top checkbox reset
        adapter.clearSelection()
    }

    override fun onBackPressed() {
        if (adapter.isSelectionMode) {
            hideSelectionUI()
        } else {
            val parent = File(currentDir).parentFile

            if (parent != null && currentDir != rootPath) {
                currentDir = parent.absolutePath
                updateFiles()
            } else {
                super.onBackPressed() // activity close
            }
        }

    }

    private fun startCopy(selectedFiles: List<File>, destDir: File) {
        showProgressDialog("Copying Files")

        FileUtils.copyFiles(selectedFiles, destDir,
            onDone = { successCount ->
                runOnUiThread {
                    progressDialog?.dismiss()
                    Toast.makeText(this, "$successCount files copied!", Toast.LENGTH_SHORT).show()
                    adapter.clearSelection()  // optional, if using multi-select
                }
            },
            updateProgress = { fileName, percent ->
                runOnUiThread {
                    val progressBar = progressDialog?.findViewById<ProgressBar>(R.id.progressBar)
                    val textView = progressDialog?.findViewById<TextView>(R.id.progressText)
                    progressBar?.progress = percent
                    textView?.text = "Copying: $fileName\n$percent%"
                }
            }
        )
    }

    private fun startMove(selectedFiles: List<File>, destDir: File) {
        showProgressDialog("Moving Files")

        FileUtils.moveFiles(selectedFiles, destDir,
            onDone = { successCount ->
                runOnUiThread {
                    progressDialog?.dismiss()
                    Toast.makeText(this, "$successCount files moved!", Toast.LENGTH_SHORT).show()
                    adapter.clearSelection()
                }
            },
            updateProgress = { fileName, percent ->
                runOnUiThread {
                    val progressBar = progressDialog?.findViewById<ProgressBar>(R.id.progressBar)
                    val textView = progressDialog?.findViewById<TextView>(R.id.progressText)
                    progressBar?.progress = percent
                    textView?.text = "Moving: $fileName\n$percent%"
                }
            }
        )
    }

    private var progressDialog: AlertDialog? = null

    private fun showProgressDialog(title: String) {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_progress, null)
        builder.setView(view)
        builder.setCancelable(false)
        progressDialog = builder.create()
        progressDialog?.show()
    }



}