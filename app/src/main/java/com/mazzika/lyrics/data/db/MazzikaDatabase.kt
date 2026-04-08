package com.mazzika.lyrics.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mazzika.lyrics.data.db.dao.FolderDao
import com.mazzika.lyrics.data.db.dao.FolderDocumentRefDao
import com.mazzika.lyrics.data.db.dao.PdfDocumentDao
import com.mazzika.lyrics.data.db.entity.FolderDocumentRefEntity
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity

@Database(
    entities = [PdfDocumentEntity::class, FolderEntity::class, FolderDocumentRefEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MazzikaDatabase : RoomDatabase() {
    abstract fun pdfDocumentDao(): PdfDocumentDao
    abstract fun folderDao(): FolderDao
    abstract fun folderDocumentRefDao(): FolderDocumentRefDao

    companion object {
        @Volatile private var INSTANCE: MazzikaDatabase? = null

        fun getInstance(context: Context): MazzikaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, MazzikaDatabase::class.java, "mazzika_lyrics.db").build()
                INSTANCE = instance
                instance
            }
        }
    }
}
