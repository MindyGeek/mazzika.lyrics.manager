package com.mazzika.lyrics.ui.catalog

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mazzika.lyrics.MazzikaApplication
import com.mazzika.lyrics.data.db.entity.FolderDocumentRefEntity
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.data.file.FileManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortMode { RECENT, TITLE }

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as MazzikaApplication).database
    private val pdfDocumentDao = database.pdfDocumentDao()
    private val folderDao = database.folderDao()
    private val folderDocumentRefDao = database.folderDocumentRefDao()
    private val fileManager = FileManager(application)

    val allFolders: StateFlow<List<FolderEntity>> = folderDao.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun hasSubFolders(folderId: Long): Boolean {
        return folderDao.getSubFolderCount(folderId) > 0
    }

    val searchQuery: MutableStateFlow<String> = MutableStateFlow("")
    val sortMode: MutableStateFlow<SortMode> = MutableStateFlow(SortMode.RECENT)
    val isImporting: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val documents: StateFlow<List<PdfDocumentEntity>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                sortMode.flatMapLatest { mode ->
                    when (mode) {
                        SortMode.RECENT -> pdfDocumentDao.getAll()
                        SortMode.TITLE -> pdfDocumentDao.getAllSortedByTitle()
                    }
                }
            } else {
                pdfDocumentDao.search(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSortMode(mode: SortMode) {
        sortMode.value = mode
    }

    fun importPdf(uri: Uri) {
        viewModelScope.launch {
            isImporting.value = true
            try {
                val result = fileManager.importPdf(uri)
                val existing = pdfDocumentDao.getByHash(result.fileHash)
                if (existing == null) {
                    val title = result.fileName.removeSuffix(".pdf")
                    pdfDocumentDao.insert(
                        PdfDocumentEntity(
                            title = title,
                            fileName = result.fileName,
                            filePath = result.filePath,
                            fileHash = result.fileHash,
                            pageCount = result.pageCount,
                            importedAt = System.currentTimeMillis(),
                            thumbnailPath = result.thumbnailPath,
                        ),
                    )
                }
            } catch (_: Exception) {
                // Silently ignore import errors for now
            } finally {
                isImporting.value = false
            }
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            pdfDocumentDao.deleteById(id)
        }
    }

    fun addToFolder(documentId: Long, folderId: Long) {
        viewModelScope.launch {
            val sortOrder = folderDocumentRefDao.getNextSortOrder(folderId)
            folderDocumentRefDao.insert(
                FolderDocumentRefEntity(
                    folderId = folderId,
                    documentId = documentId,
                    sortOrder = sortOrder,
                ),
            )
        }
    }
}
