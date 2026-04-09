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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.data.preferences.UserPreferences.ThemeMode
import com.mazzika.lyrics.ui.navigation.BottomNavBar
import com.mazzika.lyrics.ui.navigation.MazzikaTopBar
import com.mazzika.lyrics.ui.navigation.NavGraph
import com.mazzika.lyrics.ui.navigation.Screen
import com.mazzika.lyrics.ui.navigation.SessionBanner
import com.mazzika.lyrics.ui.navigation.bottomNavItems
import com.mazzika.lyrics.ui.navigation.getScreenInfo
import com.mazzika.lyrics.ui.sync.SyncRole
import com.mazzika.lyrics.ui.sync.SyncViewModel
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
                val isReaderScreen = currentRoute?.startsWith("reader") == true || currentRoute == Screen.ReaderSync.route
                val showBottomBar = !isReaderScreen

                val screenInfo = getScreenInfo(currentRoute)

                val syncViewModel: SyncViewModel = viewModel()
                val syncRole by syncViewModel.role.collectAsState()
                val connectedEndpoints by syncViewModel.connectedEndpoints.collectAsState()
                val selectedDocument by syncViewModel.selectedDocument.collectAsState()
                val isSessionActive = syncRole != SyncRole.NONE
                val sessionName = selectedDocument?.title ?: "Session"

                var currentFolderName by remember { mutableStateOf<String?>(null) }
                var currentFolderIcon by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(currentRoute) {
                    if (currentRoute?.startsWith("folder/") != true) {
                        currentFolderName = null
                        currentFolderIcon = null
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (screenInfo.showTopBar) {
                            Column {
                                MazzikaTopBar(
                                    title = screenInfo.title,
                                    showBackButton = screenInfo.showBackButton,
                                    onBackClick = { navController.popBackStack() },
                                    folderName = if (currentRoute?.startsWith("folder/") == true) currentFolderName else null,
                                    folderIcon = if (currentRoute?.startsWith("folder/") == true) currentFolderIcon else null,
                                )
                                SessionBanner(
                                    isVisible = isSessionActive,
                                    role = syncRole,
                                    sessionName = sessionName,
                                    connectedCount = connectedEndpoints.size,
                                    isConnectionLost = false,
                                    onClick = {
                                        val doc = selectedDocument
                                        if (syncRole == SyncRole.PILOT && doc != null) {
                                            navController.navigate(Screen.Reader.createRoute(doc.id))
                                        } else if (syncRole == SyncRole.FOLLOWER) {
                                            navController.navigate(Screen.ReaderSync.route)
                                        }
                                    },
                                )
                            }
                        }
                    },
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onFolderChanged = { name, icon ->
                            currentFolderName = name
                            currentFolderIcon = icon
                        },
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
