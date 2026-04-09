package com.mazzika.lyrics.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Catalog : Screen("catalog")
    object Sync : Screen("sync")
    object Settings : Screen("settings")
    object FolderDetail : Screen("folder/{folderId}") {
        fun createRoute(folderId: Long) = "folder/$folderId"
    }
    object Reader : Screen("reader/{documentId}") {
        fun createRoute(documentId: Long) = "reader/$documentId"
    }
    object ReaderSync : Screen("reader_sync")
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen,
)

val bottomNavItems = listOf(
    BottomNavItem(label = "Accueil", icon = Icons.Filled.Home, screen = Screen.Home),
    BottomNavItem(label = "Catalogue", icon = Icons.Filled.LibraryMusic, screen = Screen.Catalog),
    BottomNavItem(label = "Sync", icon = Icons.Filled.SyncAlt, screen = Screen.Sync),
    BottomNavItem(label = "Réglages", icon = Icons.Filled.Settings, screen = Screen.Settings),
)
