package com.mazzika.lyrics.ui.folders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mazzika.lyrics.MazzikaApplication
import com.mazzika.lyrics.data.db.entity.FolderDocumentRefEntity
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FolderDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val database = (application as MazzikaApplication).database
    private val folderDao = database.folderDao()
    private val folderDocumentRefDao = database.folderDocumentRefDao()

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
}
