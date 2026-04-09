package com.mazzika.lyrics.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mazzika.lyrics.MazzikaApplication
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as MazzikaApplication).database
    private val folderDao = database.folderDao()
    private val pdfDocumentDao = database.pdfDocumentDao()

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
}
