package com.mazzika.lyrics.data.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class FileManager(private val context: Context) {

    private val pdfsDir: File get() = File(context.filesDir, "pdfs").also { it.mkdirs() }
    private val thumbnailsDir: File get() = File(context.filesDir, "thumbnails").also { it.mkdirs() }
    private val tempDir: File get() = File(context.filesDir, "temp").also { it.mkdirs() }

    suspend fun importPdf(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val fileName = getFileName(uri)
        val destFile = File(pdfsDir, "${System.currentTimeMillis()}_$fileName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Cannot open file: $uri")
        val hash = computeHash(destFile)
        val pageCount = getPageCount(destFile)
        val thumbnailPath = generateThumbnail(destFile, hash)
        ImportResult(fileName = fileName, filePath = destFile.absolutePath, fileHash = hash, pageCount = pageCount, thumbnailPath = thumbnailPath)
    }

    suspend fun saveTempFile(bytes: ByteArray, fileName: String): String = withContext(Dispatchers.IO) {
        val destFile = File(tempDir, "${System.currentTimeMillis()}_$fileName")
        destFile.writeBytes(bytes)
        destFile.absolutePath
    }

    suspend fun moveTempToCatalog(tempPath: String): String = withContext(Dispatchers.IO) {
        val tempFile = File(tempPath)
        val destFile = File(pdfsDir, tempFile.name)
        tempFile.renameTo(destFile)
        destFile.absolutePath
    }

    suspend fun computeHash(file: File): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) { digest.update(buffer, 0, bytesRead) }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun getPageCount(file: File): Int = withContext(Dispatchers.IO) {
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        val count = renderer.pageCount
        renderer.close()
        fd.close()
        count
    }

    suspend fun generateThumbnail(file: File, hash: String): String = withContext(Dispatchers.IO) {
        val thumbFile = File(thumbnailsDir, "$hash.png")
        if (thumbFile.exists()) return@withContext thumbFile.absolutePath
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        val page = renderer.openPage(0)
        val width = 200
        val height = (width.toFloat() / page.width * page.height).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        FileOutputStream(thumbFile).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 85, out) }
        page.close()
        renderer.close()
        fd.close()
        bitmap.recycle()
        thumbFile.absolutePath
    }

    fun deleteFile(path: String) { File(path).delete() }
    fun readFileBytes(path: String): ByteArray = File(path).readBytes()

    private fun getFileName(uri: Uri): String {
        var name = "document.pdf"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) { name = cursor.getString(nameIndex) }
        }
        return name
    }

    data class ImportResult(val fileName: String, val filePath: String, val fileHash: String, val pageCount: Int, val thumbnailPath: String)
}
