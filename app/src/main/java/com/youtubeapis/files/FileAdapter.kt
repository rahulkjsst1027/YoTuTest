package com.youtubeapis.files

import android.annotation.SuppressLint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.youtubeapis.R
import com.youtubeapis.files.FileTypeHelper.loadFolderWithBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("NotifyDataSetChanged")
class FileAdapter(
    private val onClick: (File) -> Unit,
    private val onLongClick: (File) -> Unit,
    private val onSelectionChange: () -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        setHasStableIds(true) // tell RecyclerView items have stable IDs
    }
    // ✅ AsyncListDiffer instead of manual list
    private val differ = AsyncListDiffer(this, FileDiffCallback())


    private val selectedFiles = mutableSetOf<File>()
    fun getSelectedFiles(): List<File> = selectedFiles.toList()

    var isSelectionMode = false


    override fun getItemId(position: Int): Long {
        // har File ka unique ID
        return differ.currentList[position].absolutePath.hashCode().toLong()
    }

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileIcon: ImageView = view.findViewById(R.id.fileIcon1)
        val name: TextView = view.findViewById(R.id.fileName)
        val info: TextView = view.findViewById(R.id.items)
        val checkBox: CheckBox = view.findViewById(R.id.fileCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
       /* var context = holder.itemView.context
        val file = files[position]*/

        val context = holder.itemView.context
        val file = differ.currentList[position] // ✅ ye lo differ se

        holder.name.text = file.name

        // checkbox visibility selection mode ke hisaab se
        holder.checkBox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        // First remove old listener to avoid recycling issues
        holder.checkBox.setOnCheckedChangeListener(null)

// Set checkbox state based on selection
        holder.checkBox.isChecked = selectedFiles.contains(file)

// Item checkbox listener inside adapter
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedFiles.add(file)
            else selectedFiles.remove(file)
            onSelectionChange()  // update top checkbox in Activity
        }

// Item click listener inside adapter (already hai)
        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                val newState = !holder.checkBox.isChecked
                holder.checkBox.isChecked = newState
                // do NOT call selectAll/unselectAll here
                // just update selection
            } else {
                onClick(file)
            }
        }

        // long press
        holder.itemView.setOnLongClickListener {
            onLongClick(file)
            true
        }

        if (file.isDirectory) {
            val count = file.listFiles()?.size ?: 0
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm a", Locale.getDefault())
                .format(Date(file.lastModified()))
            holder.info.text = "$count items | $date"

            val bitmap = loadFolderWithBadge(file, context)
            if (bitmap != null) {
                Glide.with(context)
                    .load(bitmap)
                    .into(holder.fileIcon)
            }else{
                Glide.with(context)
                    .load(R.drawable.folder)
                    .into(holder.fileIcon)
            }
        }
        else {
            val sizeInMB = String.format("%.2f MB", file.length().toDouble() / (1024 * 1024))
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm a", Locale.getDefault())
                .format(Date(file.lastModified()))
            holder.info.text = "$sizeInMB | $date"

            if (FileTypeHelper.isImage(file)) {
                Glide.with(context)
                    .load(Uri.fromFile(file))
                    .override(100, 100)
                    //.diskCacheStrategy(DiskCacheStrategy.ALL) // cache original + resized
                    //.skipMemoryCache(false)
                    .transform(MultiTransformation(CenterCrop(), RoundedCorners(16)))
                    .placeholder(R.drawable.picture)
                    .into(holder.fileIcon)
            } else if (FileTypeHelper.isVideo(file)) {
                Glide.with(context)
                    .load(Uri.fromFile(file))
                    .override(100, 100)
                   // .diskCacheStrategy(DiskCacheStrategy.ALL) // cache original + resized
                   // .skipMemoryCache(false)
                    .transform(MultiTransformation(CenterCrop(), RoundedCorners(16)))
                    .placeholder(R.drawable.picture)
                    .into(holder.fileIcon)
            } else {
                Glide.with(context)
                    .load(FileTypeHelper.getFileIcon(file, context))
                    .placeholder(R.drawable.docs)
                    .into(holder.fileIcon)
            }
        }

       // holder.itemView.setOnClickListener { onClick(file) }
    }

    fun toggleSelection(file: File) {
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file)
        } else {
            selectedFiles.add(file)
        }
        onSelectionChange()
    }


    fun clearSelection() {
        selectedFiles.clear()
        isSelectionMode = false
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedFiles.clear()
        selectedFiles.addAll(differ.currentList)
        notifyDataSetChanged()
    }

    fun unselectAll() {
        selectedFiles.clear()
        notifyDataSetChanged()
    }

    // Adapter ke andar helper functions
    //fun getSelectedSize(): Long = selectedFiles.sumOf { it.length() }
    fun getSelectedCount(): Int = selectedFiles.size



    fun isAllSelected(): Boolean = selectedFiles.size == differ.currentList.size && differ.currentList.isNotEmpty()

    override fun getItemCount(): Int = differ.currentList.size // ✅ differ list ka size


    fun clearFiles() {
        differ.submitList(emptyList())
    }
    // ✅ Use DiffUtil instead of full notifyDataSetChanged
    /*fun updateFiles1(newFiles: List<File>) {
        val updatedList = files.toMutableList().apply { addAll(newFiles) }
        val diffCallback = FileDiffCallback(files, updatedList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        files = updatedList
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateFiles(newFiles: List<File>) {
        // 1️⃣ Add new files to current list
        val updatedList = files.toMutableList().apply { addAll(newFiles) }

        // 2️⃣ Custom sorting: folders A→Z, files latest first
        val sortedList = updatedList.sortedWith(
            compareBy<File> { !it.isDirectory } // folders first
                .thenBy { if (it.isDirectory) it.name.lowercase() else "" } // folders alphabetical
                //.thenByDescending { if (!it.isDirectory) it.lastModified() else 0L } // files latest first
        )

        // 3️⃣ DiffUtil calculate difference between old and new sorted list
        val diffCallback = FileDiffCallback(files, sortedList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // 4️⃣ Update adapter's list and notify
        files = sortedList
        diffResult.dispatchUpdatesTo(this)
    }*/

    fun updateFiles(newFiles: List<File>) {
        // Sorting: folders A→Z, files latest first
        val sortedList = newFiles.sortedWith(
            compareBy<File> { !it.isDirectory }
                .thenBy { if (it.isDirectory) it.name.lowercase() else "" }
               // .thenByDescending { if (!it.isDirectory) it.lastModified() else 0L }
        )
        differ.submitList(sortedList){
            selectedFiles.retainAll(sortedList.toSet())
        }
    }





    var currentSort: SortOption = SortOption.NAME
        private set

    private var currentOrder: SortOrder = SortOrder.ASCENDING


    fun setSortOption(
        option: SortOption,
        order: SortOrder = currentOrder,
        recyclerView: RecyclerView,
        progressBar: ProgressBar // 👈 pass karo loader
    ) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val visiblePos = layoutManager.findFirstVisibleItemPosition()
        val offset = layoutManager.findViewByPosition(visiblePos)?.top ?: 0

        currentSort = option
        currentOrder = order

        (recyclerView.context as? AppCompatActivity)?.lifecycleScope?.launch {
            val currentList = differ.currentList.toList() // immutable copy

            // 👇 loader show before sorting
            progressBar.visibility = View.VISIBLE
           // recyclerView.visibility = View.GONE

            val sortedList = withContext(Dispatchers.Default) {
                val comparator = when (currentSort) {
                    SortOption.NAME -> Comparator<File> { a, b ->
                        a.name.compareTo(b.name, ignoreCase = true)
                    }
                    SortOption.DATE -> Comparator<File> { a, b ->
                        (a.lastModified() - b.lastModified()).let {
                            if (it < 0) -1 else if (it > 0) 1 else 0
                        }
                    }
                    SortOption.TYPE -> Comparator<File> { a, b ->
                        a.extension.compareTo(b.extension, ignoreCase = true)
                    }
                    SortOption.SIZE -> Comparator<File> { a, b ->
                        (a.length() - b.length()).let {
                            if (it < 0) -1 else if (it > 0) 1 else 0
                        }
                    }
                }
                if (order == SortOrder.ASCENDING) currentList.sortedWith(comparator)
                else currentList.sortedWith(comparator.reversed())
            }


            withContext(Dispatchers.Main) {
                differ.submitList(null) // 👈 clear karo pehle (DiffUtil skip hoga)
                differ.submitList(ArrayList(sortedList)) { // ✅ force new instance
                    selectedFiles.retainAll(sortedList.toSet())
                    layoutManager.scrollToPositionWithOffset(visiblePos, offset)

                    // 👇 loader hide after sorted list applied
                    progressBar.visibility = View.GONE
                   // recyclerView.visibility = View.VISIBLE
                }
            }
            // update adapter
            /*differ.submitList(ArrayList(sortedList)) {
                selectedFiles.retainAll(sortedList.toSet())
                layoutManager.scrollToPositionWithOffset(visiblePos, offset)
            }*/
        }
    }


    fun setSortOption2(
        option: SortOption,
        order: SortOrder = currentOrder,
        recyclerView: RecyclerView
    ) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val visiblePos = layoutManager.findFirstVisibleItemPosition()
        val offset = layoutManager.findViewByPosition(visiblePos)?.top ?: 0

        currentSort = option
        currentOrder = order

        (recyclerView.context as? AppCompatActivity)?.lifecycleScope?.launch {
            val currentList = differ.currentList.toList() // ✅ immutable copy
            val sortedList = withContext(Dispatchers.Default) {
                val base = when (currentSort) {
                    SortOption.NAME -> currentList.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                    SortOption.DATE -> currentList.sortedBy { it.lastModified() }
                    SortOption.TYPE -> currentList.sortedBy { it.extension.lowercase() }
                    SortOption.SIZE -> currentList.sortedBy { it.length() }
                }
                if (currentOrder == SortOrder.ASCENDING) base else base.asReversed()
            }

            withContext(Dispatchers.Main) {
                differ.submitList(null) // 👈 clear karo pehle (DiffUtil skip hoga)
                differ.submitList(ArrayList(sortedList)) { // ✅ force new instance
                    selectedFiles.retainAll(sortedList.toSet())
                    layoutManager.scrollToPositionWithOffset(visiblePos, offset)
                }
            }
        }
    }




}

class FileDiffCallback : DiffUtil.ItemCallback<File>() {
    override fun areItemsTheSame(oldItem: File, newItem: File): Boolean =
        oldItem.absolutePath == newItem.absolutePath

    override fun areContentsTheSame(oldItem: File, newItem: File): Boolean =
        oldItem.lastModified() == newItem.lastModified() &&
                oldItem.length() == newItem.length()
}




