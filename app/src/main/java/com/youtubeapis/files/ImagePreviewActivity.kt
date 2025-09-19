package com.youtubeapis.files

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.text.format.DateFormat
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.youtubeapis.R
import com.youtubeapis.files.FileClickHandler.getFileType
import java.io.File
import java.io.FileOutputStream

class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var topBar: LinearLayout
    private lateinit var bottomBar: LinearLayout
    private lateinit var photoView: PhotoView
    private lateinit var tvCount: TextView
    private lateinit var btnMenu: ImageView

    private var barsVisible = true

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        topBar = findViewById(R.id.topBar)
        bottomBar = findViewById(R.id.bottomBar)
        photoView = findViewById(R.id.photoView)
        tvCount = findViewById(R.id.tvCount)
        btnMenu = findViewById(R.id.btnMenu)

        val file: File? = when {
            intent.hasExtra("data") -> {
                // internal file:// path
                intent.getStringExtra("data")?.let { File(it) }
            }
            intent.data != null -> {
                val uri = intent.data!!
                when (uri.scheme) {
                    "file" -> File(uri.path!!) // direct file
                    "content" -> {
                        // content:// URI → temp file
                        val inputStream = contentResolver.openInputStream(uri)
                        val tempFile = File(cacheDir, "temp_${System.currentTimeMillis()}.jpg")
                        inputStream?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
                        tempFile
                    }
                    else -> null
                }
            }
            else -> null
        }

        if (file == null){
            Toast.makeText(applicationContext, "File Not Support!", Toast.LENGTH_SHORT).show()
            finish()
        }

        tvCount.text = "1/1"
        Glide.with(this)
            .load(file)
            .into(photoView)

        // Toggle bars on image click
        photoView.setOnClickListener {
            barsVisible = !barsVisible
            topBar.visibility = if (barsVisible) View.VISIBLE else View.GONE
            bottomBar.visibility = if (barsVisible) View.VISIBLE else View.GONE
        }

        // Double tap handled by PhotoView automatically (zoom in/out)
        // Optional: punch / pinch zoom handled by PhotoView library

        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Menu popup
        btnMenu.setOnClickListener { v ->
            val popup = PopupMenu(this, v)
            popup.menu.add("Convert to PDF").setOnMenuItemClickListener {
                convertImageToPDF(file!!)  // file = current File object
                true
            }
            popup.menu.add("Use as").setOnMenuItemClickListener {
                useFileAs(file!!)
                true
            }
            popup.menu.add("Rename").setOnMenuItemClickListener {
                showRenameDialog(file!!)
                true
            }
            popup.menu.add("Feedback")
            popup.show()
        }

        // Bottom buttons
        findViewById<ImageView>(R.id.btnShare).setOnClickListener {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file!!
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share image via"))
        }
        findViewById<ImageView>(R.id.btnInfo).setOnClickListener {

            val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.bottom_sheet_image_info, null)

            val tvName = view.findViewById<TextView>(R.id.tvName)
            val tvType = view.findViewById<TextView>(R.id.tvType)
            val tvSize = view.findViewById<TextView>(R.id.tvSize)
            val tvDate = view.findViewById<TextView>(R.id.tvDate)
            val tvPath = view.findViewById<TextView>(R.id.tvPath)


            file?.let {
                tvName.text = it.name
                tvType.text = getString(R.string.type, getFileType(it)) // now shows Image / Video / PDF etc
                val sizeInMB = it.length().toDouble() / (1024 * 1024)
                tvSize.text = getString(R.string.size_2f_mb).format(sizeInMB)
                tvDate.text = getString(
                    R.string.modified,
                    DateFormat.format("dd MMM yyyy hh:mm a", it.lastModified())
                )
                tvPath.text = getString(R.string.path, it.absolutePath)
            }

            bottomSheet.setContentView(view)
            bottomSheet.show()

        }
        findViewById<ImageView>(R.id.btnDelete).setOnClickListener {
            deleteFile(file!!) {
                finish()
            }
        }
    }
    private fun deleteFile(file: File, onDeleted: () -> Unit) {
        if (!file.exists()) return

        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete ${file.name}?")
            .setPositiveButton("Delete") { dialog, _ ->
                if (file.delete()) {
                    Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show()
                    onDeleted()  // callback to refresh UI
                } else {
                    Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun convertImageToPDF(file: File) {
        if (!file.exists()) return

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)

        // Save PDF to Downloads folder
        val pdfFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "${file.nameWithoutExtension}.pdf")
        try {
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()
            Toast.makeText(this, "PDF saved: ${pdfFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "PDF conversion failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun useFileAs(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_ATTACH_DATA)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setDataAndType(uri, "*/*") // generic MIME type

        // Optional: chooser title
        startActivity(Intent.createChooser(intent, "Use as"))
    }

    private fun showRenameDialog(file: File) {
        val editText = EditText(this)
        editText.setText(file.nameWithoutExtension)  // editable part only
        editText.setSelection(editText.text.length)

        AlertDialog.Builder(this)
            .setTitle("Rename file")
            .setMessage("Extension .${file.extension} will remain unchanged")
            .setView(editText)
            .setPositiveButton("Rename") { dialog, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val newFile = File(file.parent, "$newName.${file.extension}") // extension fixed
                    if (file.renameTo(newFile)) {
                        Toast.makeText(this, "Renamed successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


}
