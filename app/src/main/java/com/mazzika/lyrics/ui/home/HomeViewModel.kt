package com.mazzika.lyrics.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MazzikaApplication
    private val database = app.database
    private val folderDao = database.folderDao()
    private val pdfDocumentDao = database.pdfDocumentDao()
    private val folderDocumentRefDao = database.folderDocumentRefDao()
    private val fileManager = FileManager(application)

    val rootFolders: StateFlow<List<FolderEntity>> = folderDao.getRootFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val recentDocuments: StateFlow<List<PdfDocumentEntity>> = pdfDocumentDao.getRecent(10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val allFolders: StateFlow<List<FolderEntity>> = folderDao.getAllFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    fun createFolder(name: String, icon: String?) {
        viewModelScope.launch {
            folderDao.insert(
                FolderEntity(
                    name = name,
                    icon = icon,
                    parentFolderId = null,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            folderDao.deleteById(folderId)
        }
    }

    fun renameFolder(folderId: Long, newName: String) {
        viewModelScope.launch {
            val folder = folderDao.getById(folderId) ?: return@launch
            folderDao.update(folder.copy(name = newName))
        }
    }

    /** Import a PDF to catalog only. */
    fun importToCatalog(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
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
            } finally {
                _isImporting.value = false
            }
        }
    }

    /** Import a PDF to catalog and then add it to a folder. */
    fun importToFolder(uri: Uri, folderId: Long) {
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
                    val sortOrder = folderDocumentRefDao.getNextSortOrder(folderId)
                    folderDocumentRefDao.insert(
                        FolderDocumentRefEntity(
                            folderId = folderId,
                            documentId = existing.id,
                            sortOrder = sortOrder,
                        ),
                    )
                }
            } catch (_: Exception) {
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun getSubFolders(parentId: Long) = folderDao.getSubFolders(parentId)

    suspend fun hasSubFolders(folderId: Long): Boolean {
        return folderDao.getSubFolderCount(folderId) > 0
    }
}
