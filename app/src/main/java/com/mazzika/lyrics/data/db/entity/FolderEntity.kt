package com.mazzika.lyrics.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    foreignKeys = [ForeignKey(entity = FolderEntity::class, parentColumns = ["id"], childColumns = ["parentFolderId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("parentFolderId")],
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String? = null,
    val parentFolderId: Long? = null,
    val createdAt: Long,
)
