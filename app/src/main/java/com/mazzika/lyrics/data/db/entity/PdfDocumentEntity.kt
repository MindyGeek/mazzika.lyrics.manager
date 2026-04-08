package com.mazzika.lyrics.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_documents")
data class PdfDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val fileName: String,
    val filePath: String,
    val fileHash: String,
    val pageCount: Int,
    val importedAt: Long,
    val thumbnailPath: String,
)
