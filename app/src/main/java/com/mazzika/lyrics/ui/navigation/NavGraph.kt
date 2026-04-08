package com.mazzika.lyrics.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mazzika.lyrics.ui.catalog.CatalogScreen
import com.mazzika.lyrics.ui.folders.FolderDetailScreen
import com.mazzika.lyrics.ui.home.HomeScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
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
            PlaceholderScreen(name = "Sync")
        }

        composable(Screen.Settings.route) {
            PlaceholderScreen(name = "Réglages")
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
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getLong("documentId") ?: -1L
            PlaceholderScreen(name = "Lecteur #$documentId")
        }

        composable(
            route = Screen.ReaderSync.route,
            arguments = listOf(
                navArgument("filePath") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val filePath = backStackEntry.arguments?.getString("filePath") ?: ""
            PlaceholderScreen(name = "Lecteur Sync: $filePath")
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
