package com.mazzika.lyrics.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mazzika.lyrics.data.db.entity.FolderDocumentRefEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDocumentRefDao {
    @Query("SELECT d.* FROM pdf_documents d INNER JOIN folder_document_refs r ON d.id = r.documentId WHERE r.folderId = :folderId ORDER BY r.sortOrder ASC")
    fun getDocumentsInFolder(folderId: Long): Flow<List<PdfDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ref: FolderDocumentRefEntity)

    @Query("DELETE FROM folder_document_refs WHERE folderId = :folderId AND documentId = :documentId")
    suspend fun delete(folderId: Long, documentId: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM folder_document_refs WHERE folderId = :folderId")
    suspend fun getNextSortOrder(folderId: Long): Int
}
