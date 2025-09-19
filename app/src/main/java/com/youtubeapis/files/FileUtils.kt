package com.youtubeapis.files

import java.io.File

object FileUtils {

    private fun copyFile(
        src: File,
        dest: File,
        onProgress: ((copied: Long, total: Long) -> Unit)? = null
    ): Boolean {
        return try {
            val total = src.length()
            var copied: Long = 0

            src.inputStream().use { input ->
                dest.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        copied += bytes
                        onProgress?.invoke(copied, total)
                        bytes = input.read(buffer)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyDirectory(
        srcDir: File,
        destDir: File,
        onFileCopied: ((file: File) -> Unit)? = null
    ): Boolean {
        return try {
            if (!destDir.exists()) destDir.mkdirs()
            srcDir.listFiles()?.forEach { child ->
                val destChild = File(destDir, child.name)
                val success = if (child.isDirectory) {
                    copyDirectory(child, destChild, onFileCopied)
                } else {
                    copyFile(child, destChild)
                }
                if (success) onFileCopied?.invoke(child)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun moveFileOrDir(
        src: File,
        dest: File,
        onFileMoved: ((file: File) -> Unit)? = null
    ): Boolean {
        // 🔥 Step 1: Try fast move
        if (src.renameTo(dest)) {
            onFileMoved?.invoke(src)
            return true
        }

        // 🔥 Step 2: Copy + Delete
        val success = if (src.isDirectory) {
            copyDirectory(src, dest, onFileMoved)
        } else {
            copyFile(src, dest)
        }

        if (success) {
            src.deleteRecursively()
            onFileMoved?.invoke(src)
        }
        return success
    }

    fun copyFiles(selected: List<File>, destDir: File, onDone: (Int) -> Unit, updateProgress: (String, Int) -> Unit) {
        Thread {
            var successCount = 0
            for ((index, src) in selected.withIndex()) {
                val destFile = File(destDir, src.name)

                val success = if (src.isDirectory) {
                    copyDirectory(src, destFile) {
                        // ek file copy hone par
                        updateProgress("Copied: ${it.name}", ((index + 1) * 100) / selected.size)
                    }
                } else {
                    copyFile(src, destFile) { copied, total ->
                        val percent = (copied * 100 / total).toInt()
                        updateProgress("Copying ${src.name}: $percent%", percent)
                    }
                }

                if (success) successCount++
            }

            onDone(successCount)
        }.start()
    }
    fun moveFiles(selected: List<File>, destDir: File, onDone: (Int) -> Unit, updateProgress: (String, Int) -> Unit) {
        Thread {
            var successCount = 0
            for ((index, src) in selected.withIndex()) {
                val destFile = File(destDir, src.name)

                val success = moveFileOrDir(src, destFile) {
                    updateProgress("Moved: ${it.name}", ((index + 1) * 100) / selected.size)
                }

                if (success) successCount++
            }

            onDone(successCount)
        }.start()
    }

}
