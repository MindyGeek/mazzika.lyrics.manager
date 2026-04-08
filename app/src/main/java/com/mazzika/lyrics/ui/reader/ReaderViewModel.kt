package com.mazzika.lyrics.ui.reader

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mazzika.lyrics.MazzikaApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ReaderViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val dao = (application as MazzikaApplication).database.pdfDocumentDao()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _showToolbar = MutableStateFlow(true)
    val showToolbar: StateFlow<Boolean> = _showToolbar.asStateFlow()

    private val _filePath = MutableStateFlow("")
    val filePath: StateFlow<String> = _filePath.asStateFlow()

    private var renderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private val pageCache = LinkedHashMap<Int, Bitmap>(5, 0.75f, true)
    private val maxCacheSize = 3

    init {
        val documentId = savedStateHandle.get<Long>("documentId")
        if (documentId != null && documentId > 0) {
            loadDocument(documentId)
        }
    }

    private fun loadDocument(documentId: Long) {
        viewModelScope.launch {
            val doc = dao.getById(documentId) ?: return@launch
            _title.value = doc.title
            _filePath.value = doc.filePath
            openRenderer(doc.filePath)
        }
    }

    fun initWithFilePath(path: String) {
        _filePath.value = path
        val file = File(path)
        _title.value = file.nameWithoutExtension
        viewModelScope.launch {
            openRenderer(path)
        }
    }

    private suspend fun openRenderer(path: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                Log.d("ReaderViewModel", "openRenderer: path=$path, exists=${file.exists()}, size=${if (file.exists()) file.length() else -1}")
                if (!file.exists()) return@withContext
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(fd)
                synchronized(this@ReaderViewModel) {
                    fileDescriptor = fd
                    renderer = pdfRenderer
                    _pageCount.value = pdfRenderer.pageCount
                }
                Log.d("ReaderViewModel", "openRenderer: success, pageCount=${pdfRenderer.pageCount}")
            } catch (e: Exception) {
                Log.e("ReaderViewModel", "openRenderer: failed", e)
            }
        }
    }

    fun renderPage(pageIndex: Int, width: Int): Bitmap? {
        if (width <= 0) return null
        val cacheKey = pageIndex * 100000 + width
        pageCache[cacheKey]?.let { return it }

        val pdfRenderer = synchronized(this) { renderer } ?: return null
        if (pageIndex < 0 || pageIndex >= pdfRenderer.pageCount) return null

        return try {
            val page = pdfRenderer.openPage(pageIndex)
            val scale = width.toFloat() / page.width
            val height = (page.height * scale).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            // Evict oldest entries if cache is full
            while (pageCache.size >= maxCacheSize) {
                val oldest = pageCache.entries.iterator().next()
                pageCache.remove(oldest.key)
                oldest.value.recycle()
            }
            pageCache[cacheKey] = bitmap
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun setCurrentPage(page: Int) {
        if (page in 0 until _pageCount.value) {
            _currentPage.value = page
        }
    }

    fun toggleToolbar() {
        _showToolbar.value = !_showToolbar.value
    }

    override fun onCleared() {
        super.onCleared()
        pageCache.values.forEach { it.recycle() }
        pageCache.clear()
        try {
            renderer?.close()
        } catch (_: Exception) {
        }
        try {
            fileDescriptor?.close()
        } catch (_: Exception) {
        }
    }
}
