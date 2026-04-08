package com.mazzika.lyrics.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDocumentDao {
    @Query("SELECT * FROM pdf_documents ORDER BY importedAt DESC")
    fun getAll(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents WHERE id = :id")
    suspend fun getById(id: Long): PdfDocumentEntity?

    @Query("SELECT * FROM pdf_documents WHERE fileHash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): PdfDocumentEntity?

    @Query("SELECT * FROM pdf_documents WHERE title LIKE '%' || :query || '%' ORDER BY importedAt DESC")
    fun search(query: String): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents ORDER BY importedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 10): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents ORDER BY title ASC")
    fun getAllSortedByTitle(): Flow<List<PdfDocumentEntity>>

    @Insert
    suspend fun insert(document: PdfDocumentEntity): Long

    @Update
    suspend fun update(document: PdfDocumentEntity)

    @Query("DELETE FROM pdf_documents WHERE id = :id")
    suspend fun deleteById(id: Long)
}
