package com.mazzika.lyrics

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.data.preferences.UserPreferences.ThemeMode
import com.mazzika.lyrics.ui.navigation.BottomNavBar
import com.mazzika.lyrics.ui.navigation.NavGraph
import com.mazzika.lyrics.ui.navigation.bottomNavItems
import com.mazzika.lyrics.ui.theme.MazzikaLyricsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        setContent {
            val app = application as MazzikaApplication
            val themeMode by app.userPreferences.theme.collectAsState(initial = ThemeMode.SYSTEM)
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemInDarkTheme
            }

            MazzikaLyricsTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val mainTabRoutes = bottomNavItems.map { it.screen.route }.toSet()
                val showBottomBar = currentRoute in mainTabRoutes

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it }),
                        ) {
                            BottomNavBar(navController = navController)
                        }
                    },
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "application/pdf") {
            val uri: Uri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) ?: return
            val app = application as MazzikaApplication
            lifecycleScope.launch {
                val result = app.fileManager.importPdf(uri)
                val entity = PdfDocumentEntity(
                    title = result.fileName.removeSuffix(".pdf"),
                    fileName = result.fileName,
                    filePath = result.filePath,
                    fileHash = result.fileHash,
                    pageCount = result.pageCount,
                    importedAt = System.currentTimeMillis(),
                    thumbnailPath = result.thumbnailPath,
                )
                app.database.pdfDocumentDao().insert(entity)
            }
        }
    }
}
