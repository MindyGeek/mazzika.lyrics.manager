package com.mazzika.lyrics.ui.folders

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mazzika.lyrics.MazzikaApplication
import com.mazzika.lyrics.data.db.entity.FolderDocumentRefEntity
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.data.file.FileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FolderDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val app = application as MazzikaApplication
    private val database = app.database
    private val folderDao = database.folderDao()
    private val folderDocumentRefDao = database.folderDocumentRefDao()
    private val pdfDocumentDao = database.pdfDocumentDao()
    private val fileManager = FileManager(application)

    private val folderId: Long = checkNotNull(savedStateHandle["folderId"])

    val folder: StateFlow<FolderEntity?> = flow {
        emit(folderDao.getById(folderId))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val subFolders: StateFlow<List<FolderEntity>> = folderDao.getSubFolders(folderId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val documents: StateFlow<List<PdfDocumentEntity>> = folderDocumentRefDao.getDocumentsInFolder(folderId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** All catalog documents for the "copy from catalog" feature. */
    val allCatalogDocuments: StateFlow<List<PdfDocumentEntity>> = pdfDocumentDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    fun createSubFolder(name: String, icon: String?) {
        viewModelScope.launch {
            folderDao.insert(
                FolderEntity(
                    name = name,
                    icon = icon,
                    parentFolderId = folderId,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun removeDocumentFromFolder(documentId: Long) {
        viewModelScope.launch {
            folderDocumentRefDao.delete(folderId, documentId)
        }
    }

    fun deleteSubFolder(subFolderId: Long) {
        viewModelScope.launch {
            folderDao.deleteById(subFolderId)
        }
    }

    fun renameSubFolder(subFolderId: Long, newName: String) {
        viewModelScope.launch {
            folderDao.getById(subFolderId)?.let { folder ->
                folderDao.update(folder.copy(name = newName))
            }
        }
    }

    fun addDocumentToFolder(documentId: Long) {
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

    /** Import a PDF file: add to catalog (dedup by hash) + add ref to current folder. */
    fun importFileToFolder(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val result = fileManager.importPdf(uri)
                var existing = pdfDocumentDao.getByHash(result.fileHash)
                if (existing == null) {
                    val title = result.fileName.removeSuffix(".pdf")
                    val id = pdfDocumentDao.insert(
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
                    existing = pdfDocumentDao.getById(id)
                }
                if (existing != null) {
                    addDocumentToFolder(existing.id)
                }
            } catch (_: Exception) {
            } finally {
                _isImporting.value = false
            }
        }
    }
}
