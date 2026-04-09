package com.mazzika.lyrics

import android.app.Application
import com.mazzika.lyrics.data.db.MazzikaDatabase
import com.mazzika.lyrics.data.file.FileManager
import com.mazzika.lyrics.data.preferences.UserPreferences

class MazzikaApplication : Application() {
    val database: MazzikaDatabase by lazy { MazzikaDatabase.getInstance(this) }
    val fileManager: FileManager by lazy { FileManager(this) }
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }
}
