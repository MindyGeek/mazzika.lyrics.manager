# Header Fixe + Bandeau de Session — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ajouter un header fixe (Top App Bar Material 3) sur toutes les pages et un bandeau de session persistant visible partout quand une session sync est active.

**Architecture:** Le header et le bandeau sont placés dans le Scaffold de MainActivity (au-dessus du NavGraph). Le titre et le bouton retour changent dynamiquement selon la route active. Le bandeau observe l'état de session du SyncViewModel (activity-scoped). Les écrans individuels n'ont plus besoin de gérer leur propre header.

**Tech Stack:** Jetpack Compose, Material 3 TopAppBar, Compose Navigation, StateFlow

**Spec:** `docs/superpowers/specs/2026-04-09-ux-refonte-design.md` (sections 2 et 4.2)

---

## File Structure

```
app/src/main/java/com/mazzika/lyrics/
├── MainActivity.kt                              (modify — add topBar with header + banner to Scaffold)
├── ui/
│   ├── navigation/
│   │   ├── TopBar.kt                            (create — MazzikaTopBar composable)
│   │   ├── SessionBanner.kt                     (create — persistent session banner)
│   │   ├── Screen.kt                            (modify — add screen titles and back button info)
│   │   └── NavGraph.kt                          (modify — remove FolderDetail's internal TopAppBar)
│   ├── home/
│   │   └── HomeScreen.kt                        (modify — remove internal header)
│   ├── catalog/
│   │   └── CatalogScreen.kt                     (modify — remove internal header)
│   ├── sync/
│   │   └── SyncScreen.kt                        (modify — remove internal title)
│   ├── settings/
│   │   └── SettingsScreen.kt                    (modify — remove internal title)
│   └── folders/
│       └── FolderDetailScreen.kt                (modify — remove internal Scaffold+TopAppBar)
```

---

### Task 1: Screen metadata — titres et navigation retour

**Files:**
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/navigation/Screen.kt`

- [ ] **Step 1: Add title and canGoBack properties to Screen**

Add a helper function that returns the screen title and whether a back button should be shown, based on the current route:

```kotlin
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

data class ScreenInfo(
    val title: String,
    val showBackButton: Boolean,
    val showTopBar: Boolean,
)

fun getScreenInfo(route: String?): ScreenInfo {
    return when {
        route == null -> ScreenInfo("Mazzika", showBackButton = false, showTopBar = true)
        route == Screen.Home.route -> ScreenInfo("Mazzika", showBackButton = false, showTopBar = true)
        route == Screen.Catalog.route -> ScreenInfo("Catalogue", showBackButton = false, showTopBar = true)
        route == Screen.Sync.route -> ScreenInfo("Session", showBackButton = false, showTopBar = true)
        route == Screen.Settings.route -> ScreenInfo("Paramètres", showBackButton = false, showTopBar = true)
        route.startsWith("folder/") -> ScreenInfo("Dossier", showBackButton = true, showTopBar = true)
        route.startsWith("reader/") -> ScreenInfo("", showBackButton = false, showTopBar = false)
        route == Screen.ReaderSync.route -> ScreenInfo("", showBackButton = false, showTopBar = false)
        else -> ScreenInfo("Mazzika", showBackButton = false, showTopBar = true)
    }
}
```

- [ ] **Step 2: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 2: MazzikaTopBar — Header fixe Material 3

**Files:**
- Create: `app/src/main/java/com/mazzika/lyrics/ui/navigation/TopBar.kt`

- [ ] **Step 1: Create MazzikaTopBar composable**

```kotlin
package com.mazzika.lyrics.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mazzika.lyrics.ui.theme.DarkBackground
import com.mazzika.lyrics.ui.theme.DarkTextPrimary
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.PlayfairDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MazzikaTopBar(
    title: String,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    folderName: String? = null,
    folderIcon: String? = null,
) {
    TopAppBar(
        title = {
            if (folderName != null) {
                // Folder detail: show icon + folder name
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    if (folderIcon != null) {
                        Text(
                            text = folderIcon,
                            fontSize = 20.sp,
                            modifier = androidx.compose.ui.Modifier.padding(end = androidx.compose.ui.unit.dp(8)),
                        )
                    }
                    Text(
                        text = folderName,
                        color = DarkTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            } else if (title == "Mazzika") {
                // Home: show branded title
                Text(
                    text = title,
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Gold,
                )
            } else {
                // Other tabs: simple title
                Text(
                    text = title,
                    color = DarkTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                )
            }
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = Gold,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkBackground,
            titleContentColor = DarkTextPrimary,
        ),
    )
}
```

- [ ] **Step 2: Fix imports — use proper Modifier and dp references**

Replace the inline references with proper imports. The file should have these imports at the top:

```kotlin
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
```

And the body should use `Modifier.padding(end = 8.dp)` instead of the inline references.

- [ ] **Step 3: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 3: SessionBanner — Bandeau de session persistant

**Files:**
- Create: `app/src/main/java/com/mazzika/lyrics/ui/navigation/SessionBanner.kt`

- [ ] **Step 1: Create SessionBanner composable**

```kotlin
package com.mazzika.lyrics.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazzika.lyrics.ui.sync.SyncRole
import com.mazzika.lyrics.ui.theme.Success

@Composable
fun SessionBanner(
    isVisible: Boolean,
    role: SyncRole,
    sessionName: String,
    connectedCount: Int,
    isConnectionLost: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
    ) {
        val bgColor = if (isConnectionLost) Color(0xFFB71C1C) else Success

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .clickable(onClick = onClick)
                .height(44.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: status icon + session name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Filled.Circle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(8.dp),
                )
                Text(
                    text = if (isConnectionLost) "Connexion perdue" else sessionName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Right: connected count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.People,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "$connectedCount",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 4: Intégrer le header et le bandeau dans MainActivity

**Files:**
- Modify: `app/src/main/java/com/mazzika/lyrics/MainActivity.kt`

- [ ] **Step 1: Update MainActivity to add topBar with header + banner**

Replace the `Scaffold` section in `setContent` to include the TopBar and SessionBanner. The key changes:

1. Get `SyncViewModel` at activity scope (same pattern as NavGraph)
2. Observe route changes to determine screen title and back button
3. Add `topBar` to Scaffold with `Column { MazzikaTopBar + SessionBanner }`
4. Hide topBar on reader screens

The updated `setContent` block should be:

```kotlin
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
        val screenInfo = getScreenInfo(currentRoute)

        // Activity-scoped SyncViewModel for banner state
        val syncViewModel: SyncViewModel = viewModel()
        val syncRole by syncViewModel.role.collectAsState()
        val connectedEndpoints by syncViewModel.connectedEndpoints.collectAsState()
        val selectedDocument by syncViewModel.selectedDocument.collectAsState()

        val isSessionActive = syncRole != SyncRole.NONE
        val sessionName = selectedDocument?.title ?: "Session"

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (screenInfo.showTopBar) {
                    Column {
                        MazzikaTopBar(
                            title = screenInfo.title,
                            showBackButton = screenInfo.showBackButton,
                            onBackClick = { navController.popBackStack() },
                        )
                        SessionBanner(
                            isVisible = isSessionActive,
                            role = syncRole,
                            sessionName = sessionName,
                            connectedCount = connectedEndpoints.size,
                            isConnectionLost = false,
                            onClick = {
                                // Navigate to reader if session is active
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
            )
        }
    }
}
```

- [ ] **Step 2: Add required imports to MainActivity**

Add these imports:
```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.ui.navigation.MazzikaTopBar
import com.mazzika.lyrics.ui.navigation.SessionBanner
import com.mazzika.lyrics.ui.navigation.Screen
import com.mazzika.lyrics.ui.navigation.getScreenInfo
import com.mazzika.lyrics.ui.sync.SyncRole
import com.mazzika.lyrics.ui.sync.SyncViewModel
```

- [ ] **Step 3: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 5: Supprimer les headers internes des écrans

Maintenant que le header est global, il faut retirer les headers dupliqués dans chaque écran.

**Files:**
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/catalog/CatalogScreen.kt`
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/sync/SyncScreen.kt`
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/folders/FolderDetailScreen.kt`

- [ ] **Step 1: HomeScreen — Remove HomeHeader composable call**

In `HomeScreen.kt`, remove the `item { HomeHeader(...) }` block from the LazyColumn. The header with "Mazzika" title and sync/settings buttons is now handled by MazzikaTopBar. Keep the QuickActionChips as the first item.

Also remove the `onNavigateToSync` and `onNavigateToSettings` parameters from `HomeScreen` composable since the header no longer has those buttons (they're in the bottom nav).

- [ ] **Step 2: CatalogScreen — Remove CatalogHeader**

In `CatalogScreen.kt`, remove the `item { CatalogHeader(documentCount) }` block. The title "Catalogue" is now in the TopBar. Keep the document count as a small text under the search bar if desired, or remove it.

- [ ] **Step 3: SyncScreen — Remove title**

In `SyncScreen.kt`, in the `NoneState` composable, remove the "Synchronisation" title Text at the top. The title "Session" is now in the TopBar.

- [ ] **Step 4: SettingsScreen — Remove title**

In `SettingsScreen.kt`, remove the "Paramètres" title Text. It's now in the TopBar.

- [ ] **Step 5: FolderDetailScreen — Remove internal Scaffold+TopAppBar**

In `FolderDetailScreen.kt`:
- Remove the `Scaffold` wrapper and its `topBar` parameter
- Replace with a simple `Box` or `LazyColumn` directly
- The folder icon + name + back button are now handled by MazzikaTopBar
- The `onNavigateBack` parameter stays (used by MazzikaTopBar via NavController)

To show the folder name in the TopBar, we need to pass folder info up. The cleanest approach: in `NavGraph.kt`, observe the FolderDetailViewModel's folder state and pass folderName/folderIcon to MazzikaTopBar via a shared state or callback.

**Alternative (simpler):** Keep a simple approach where the TopBar shows "Dossier" for folder routes, and the folder name is shown inside the screen content as a subtitle. This avoids complex state hoisting.

- [ ] **Step 6: Update NavGraph — remove FolderDetailScreen navigation callbacks that are now handled by TopBar**

In `NavGraph.kt`, simplify the `FolderDetailScreen` composable call. The `onNavigateBack` is still needed (TopBar's back button calls `navController.popBackStack()`).

- [ ] **Step 7: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 6: Folder name in TopBar — état partagé

**Files:**
- Modify: `app/src/main/java/com/mazzika/lyrics/MainActivity.kt`
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Add folder state to MainActivity for TopBar**

In `NavGraph.kt`, when navigating to FolderDetail, the `FolderDetailViewModel` loads the folder. To show the folder name in the TopBar (which lives in MainActivity), use a simple approach:

Add a `MutableStateFlow` or callback mechanism. The simplest: add a `currentFolderName` and `currentFolderIcon` state in a shared place.

Option A (recommended): Add state hoisting via a lambda that NavGraph calls when the folder screen loads:

In `NavGraph.kt`, add parameters:
```kotlin
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onFolderChanged: (name: String?, icon: String?) -> Unit = { _, _ -> },
)
```

In the FolderDetail composable:
```kotlin
composable(Screen.FolderDetail.route, ...) {
    val viewModel: FolderDetailViewModel = viewModel()
    val folder by viewModel.folder.collectAsState()
    
    LaunchedEffect(folder) {
        onFolderChanged(folder?.name, folder?.icon)
    }
    
    FolderDetailScreen(...)
}
```

In `MainActivity.kt`:
```kotlin
var currentFolderName by remember { mutableStateOf<String?>(null) }
var currentFolderIcon by remember { mutableStateOf<String?>(null) }

// In topBar:
MazzikaTopBar(
    title = screenInfo.title,
    showBackButton = screenInfo.showBackButton,
    onBackClick = { navController.popBackStack() },
    folderName = if (currentRoute?.startsWith("folder/") == true) currentFolderName else null,
    folderIcon = if (currentRoute?.startsWith("folder/") == true) currentFolderIcon else null,
)

// In NavGraph call:
NavGraph(
    navController = navController,
    modifier = ...,
    onFolderChanged = { name, icon ->
        currentFolderName = name
        currentFolderIcon = icon
    },
)
```

- [ ] **Step 2: Reset folder state when leaving folder screen**

Add a `LaunchedEffect` that resets folder state when the route changes away from folder:

```kotlin
LaunchedEffect(currentRoute) {
    if (currentRoute?.startsWith("folder/") != true) {
        currentFolderName = null
        currentFolderIcon = null
    }
}
```

- [ ] **Step 3: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL
