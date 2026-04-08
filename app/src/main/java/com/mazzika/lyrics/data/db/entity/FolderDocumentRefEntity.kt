package com.mazzika.lyrics.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "folder_document_refs",
    primaryKeys = ["folderId", "documentId"],
    foreignKeys = [
        ForeignKey(entity = FolderEntity::class, parentColumns = ["id"], childColumns = ["folderId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PdfDocumentEntity::class, parentColumns = ["id"], childColumns = ["documentId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("documentId")],
)
data class FolderDocumentRefEntity(
    val folderId: Long,
    val documentId: Long,
    val sortOrder: Int,
)
