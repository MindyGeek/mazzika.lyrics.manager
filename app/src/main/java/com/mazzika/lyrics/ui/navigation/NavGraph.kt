package com.mazzika.lyrics.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.ui.catalog.CatalogScreen
import com.mazzika.lyrics.ui.folders.FolderDetailScreen
import com.mazzika.lyrics.ui.home.HomeScreen
import com.mazzika.lyrics.ui.reader.ReaderScreen
import com.mazzika.lyrics.ui.reader.ReaderViewModel
import com.mazzika.lyrics.ui.reader.SyncMode
import com.mazzika.lyrics.ui.settings.SettingsScreen
import com.mazzika.lyrics.ui.sync.SyncRole
import com.mazzika.lyrics.ui.sync.SyncScreen
import com.mazzika.lyrics.ui.sync.SyncViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    // Activity-scoped SyncViewModel so it is shared between Sync and Reader screens
    val activity = LocalContext.current as ComponentActivity
    val syncViewModel: SyncViewModel = viewModel(activity)

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToFolder = { folderId ->
                    navController.navigate(Screen.FolderDetail.createRoute(folderId))
                },
                onNavigateToReader = { documentId ->
                    navController.navigate(Screen.Reader.createRoute(documentId))
                },
                onNavigateToSync = {
                    navController.navigate(Screen.Sync.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }

        composable(Screen.Catalog.route) {
            CatalogScreen(
                onNavigateToReader = { documentId ->
                    navController.navigate(Screen.Reader.createRoute(documentId))
                },
            )
        }

        composable(Screen.Sync.route) {
            SyncScreen(
                onNavigateToReaderSync = { filePath ->
                    navController.navigate(Screen.ReaderSync.createRoute(filePath))
                },
                onNavigateToReader = { documentId ->
                    navController.navigate(Screen.Reader.createRoute(documentId))
                },
                viewModel = syncViewModel,
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(
            route = Screen.FolderDetail.route,
            arguments = listOf(
                navArgument("folderId") { type = NavType.LongType },
            ),
        ) {
            FolderDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFolder = { folderId ->
                    navController.navigate(Screen.FolderDetail.createRoute(folderId))
                },
                onNavigateToReader = { documentId ->
                    navController.navigate(Screen.Reader.createRoute(documentId))
                },
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("documentId") { type = NavType.LongType },
            ),
        ) {
            val readerViewModel: ReaderViewModel = viewModel()
            val role by syncViewModel.role.collectAsState()
            val syncMode = when (role) {
                SyncRole.PILOT -> SyncMode.PILOT
                else -> SyncMode.NONE
            }
            ReaderScreen(
                viewModel = readerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSync = { navController.navigate(Screen.Sync.route) },
                syncMode = syncMode,
                onPageChangedForSync = { page -> syncViewModel.broadcastPageChange(page) },
            )
        }

        composable(
            route = Screen.ReaderSync.route,
            arguments = listOf(
                navArgument("filePath") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val filePath = backStackEntry.arguments?.getString("filePath") ?: ""
            val readerViewModel: ReaderViewModel = viewModel()
            LaunchedEffect(filePath) {
                if (filePath.isNotEmpty()) {
                    readerViewModel.initWithFilePath(filePath)
                }
            }
            val syncPageValue by syncViewModel.syncPage.collectAsState()
            val isDetached by syncViewModel.isDetached.collectAsState()
            val isTempFile by syncViewModel.isTempFile.collectAsState()
            ReaderScreen(
                viewModel = readerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSync = { navController.navigate(Screen.Sync.route) },
                syncMode = SyncMode.FOLLOWER,
                syncPage = syncPageValue,
                isDetached = isDetached,
                onToggleDetached = { syncViewModel.toggleDetached() },
                isTempFile = isTempFile,
                onSaveToCatalogue = { syncViewModel.saveToCatalogue() },
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = name)
    }
}
