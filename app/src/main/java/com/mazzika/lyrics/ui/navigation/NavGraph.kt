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
import com.mazzika.lyrics.ui.folders.FoldersScreen
import com.mazzika.lyrics.ui.folders.FolderDetailViewModel
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
    onFolderChanged: (name: String?, icon: String?) -> Unit = { _, _ -> },
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
                onNavigateToCatalog = {
                    navController.navigate(Screen.Catalog.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }

        composable(Screen.Folders.route) {
            FoldersScreen(
                onNavigateToFolder = { folderId ->
                    navController.navigate(Screen.FolderDetail.createRoute(folderId))
                },
            )
        }

        composable(Screen.Catalog.route) {
            CatalogScreen(
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
            )
        }

        composable(Screen.Sync.route) {
            SyncScreen(
                onOpenSharedFile = {
                    navController.navigate(Screen.ReaderSync.route) {
                        launchSingleTop = true
                    }
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
            val folderDetailViewModel: FolderDetailViewModel = viewModel()
            val folder by folderDetailViewModel.folder.collectAsState()

            LaunchedEffect(folder) {
                onFolderChanged(folder?.name, folder?.icon)
            }

            FolderDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFolder = { folderId ->
                    navController.navigate(Screen.FolderDetail.createRoute(folderId))
                },
                onNavigateToReader = { documentId ->
                    navController.navigate(Screen.Reader.createRoute(documentId))
                },
                viewModel = folderDetailViewModel,
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
            val connectedEndpointsList by syncViewModel.connectedEndpoints.collectAsState()
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
                connectedCount = connectedEndpointsList.size,
                isConnectionHealthy = connectedEndpointsList.isNotEmpty(),
            )
        }

        composable(route = Screen.ReaderSync.route) {
            val syncFilePathValue by syncViewModel.syncFilePath.collectAsState()
            val readerViewModel: ReaderViewModel = viewModel()
            LaunchedEffect(syncFilePathValue) {
                val path = syncFilePathValue
                if (!path.isNullOrEmpty()) {
                    readerViewModel.initWithFilePath(path)
                }
            }
            val syncPageValue by syncViewModel.syncPage.collectAsState()
            val pilotPage by syncViewModel.pilotCurrentPage.collectAsState()
            val isDetached by syncViewModel.isDetached.collectAsState()
            val isTempFile by syncViewModel.isTempFile.collectAsState()
            val syncConnectedEndpoints by syncViewModel.connectedEndpoints.collectAsState()
            val sessionEnded by syncViewModel.sessionEndedByPilot.collectAsState()

            // Auto-close the reader when the pilot ends the session; the confirmation
            // modal is hosted by SyncScreen, which the back-stack will surface next.
            LaunchedEffect(sessionEnded) {
                if (sessionEnded) navController.popBackStack()
            }

            ReaderScreen(
                viewModel = readerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSync = { navController.navigate(Screen.Sync.route) },
                syncMode = SyncMode.FOLLOWER,
                syncPage = syncPageValue,
                pilotCurrentPage = pilotPage,
                isDetached = isDetached,
                onToggleDetached = { syncViewModel.toggleDetached() },
                isTempFile = isTempFile,
                onSaveToCatalogue = { syncViewModel.saveToCatalogue() },
                connectedCount = syncConnectedEndpoints.size,
                isConnectionHealthy = syncConnectedEndpoints.isNotEmpty(),
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
