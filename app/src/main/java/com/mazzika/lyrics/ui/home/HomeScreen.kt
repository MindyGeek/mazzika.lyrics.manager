package com.mazzika.lyrics.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.ui.theme.DarkBackground
import com.mazzika.lyrics.ui.theme.DarkSurface
import com.mazzika.lyrics.ui.theme.DarkSurfaceElevated
import com.mazzika.lyrics.ui.theme.DarkTextMuted
import com.mazzika.lyrics.ui.theme.DarkTextPrimary
import com.mazzika.lyrics.ui.theme.DarkTextSecondary
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.GoldDeep
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val folderIconOptions = listOf("📁", "🎶", "🎸", "⭐", "🎼", "✨", "📦", "🎤")

@Composable
fun HomeScreen(
    onNavigateToFolder: (Long) -> Unit,
    onNavigateToReader: (Long) -> Unit,
    onNavigateToSync: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val rootFolders by viewModel.rootFolders.collectAsState()
    val recentDocuments by viewModel.recentDocuments.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // Action cards row
            item {
                ActionCardsRow(
                    onCreateSession = onNavigateToSync,
                    onJoin = onNavigateToSync,
                    onImport = { showImportDialog = true },
                )
            }

            // Folders section
            item {
                SectionHeader(title = "Mes Dossiers")
            }

            item {
                val isTablet = LocalConfiguration.current.screenWidthDp >= 600
                if (isTablet) {
                    FolderGrid(
                        folders = rootFolders,
                        onFolderClick = { onNavigateToFolder(it.id) },
                        onDeleteFolder = { viewModel.deleteFolder(it.id) },
                        onRenameFolder = { folder, newName ->
                            viewModel.renameFolder(folder.id, newName)
                        },
                        onCreateFolder = { showCreateFolderDialog = true },
                    )
                } else {
                    FolderRow(
                        folders = rootFolders,
                        onFolderClick = { onNavigateToFolder(it.id) },
                        onDeleteFolder = { viewModel.deleteFolder(it.id) },
                        onRenameFolder = { folder, newName ->
                            viewModel.renameFolder(folder.id, newName)
                        },
                        onCreateFolder = { showCreateFolderDialog = true },
                    )
                }
            }

            // Recent files section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "Récemment ouverts")
            }

            if (recentDocuments.isEmpty()) {
                item {
                    EmptyRecentState()
                }
            } else {
                items(recentDocuments) { document ->
                    RecentFileItem(
                        document = document,
                        onClick = { onNavigateToReader(document.id) },
                    )
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { name, icon ->
                viewModel.createFolder(name, icon)
                showCreateFolderDialog = false
            },
        )
    }

    if (showImportDialog) {
        ImportDialog(
            viewModel = viewModel,
            onDismiss = { showImportDialog = false },
        )
    }
}

// --- Action Cards ---

@Composable
private fun ActionCardsRow(
    onCreateSession: () -> Unit,
    onJoin: () -> Unit,
    onImport: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActionCard(
            title = "Créer une session",
            icon = Icons.Filled.CellTower,
            onClick = onCreateSession,
            modifier = Modifier.weight(1f),
        )
        ActionCard(
            title = "Rejoindre",
            icon = Icons.Filled.Search,
            onClick = onJoin,
            modifier = Modifier.weight(1f),
        )
        ActionCard(
            title = "Importer",
            icon = Icons.Filled.UploadFile,
            onClick = onImport,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GoldDeep.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = DarkTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// --- Import Dialog ---

@Composable
private fun ImportDialog(
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
) {
    var showFolderSelector by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val catalogPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importToCatalog(uri)
            onDismiss()
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingUri = uri
            showFolderSelector = true
        }
    }

    if (showFolderSelector && pendingUri != null) {
        FolderSelectorDialog(
            viewModel = viewModel,
            onDismiss = {
                showFolderSelector = false
                pendingUri = null
                onDismiss()
            },
            onFolderSelected = { folderId ->
                pendingUri?.let { uri ->
                    viewModel.importToFolder(uri, folderId)
                }
                showFolderSelector = false
                pendingUri = null
                onDismiss()
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = DarkSurfaceElevated,
            title = { Text("Importer un fichier", color = DarkTextPrimary) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ImportChoiceCard(
                        title = "Catalogue uniquement",
                        subtitle = "Ajouter au catalogue sans ranger dans un dossier",
                        onClick = {
                            catalogPickerLauncher.launch(arrayOf("application/pdf"))
                        },
                    )
                    ImportChoiceCard(
                        title = "Dans un dossier",
                        subtitle = "Importer et ranger dans un dossier de votre choix",
                        onClick = {
                            folderPickerLauncher.launch(arrayOf("application/pdf"))
                        },
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Annuler", color = DarkTextSecondary)
                }
            },
        )
    }
}

@Composable
private fun ImportChoiceCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = DarkTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = DarkTextMuted,
                    fontSize = 12.sp,
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = DarkTextMuted,
            )
        }
    }
}

// --- Folder Selector Dialog ---

@Composable
fun FolderSelectorDialog(
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onFolderSelected: (Long) -> Unit,
) {
    val allFolders by viewModel.allFolders.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Breadcrumb navigation stack: list of (folderId?, folderName)
    // null folderId means root
    val breadcrumb = remember { mutableStateListOf<Pair<Long?, String>>() }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }

    // Initialize breadcrumb with root
    LaunchedEffect(Unit) {
        if (breadcrumb.isEmpty()) {
            breadcrumb.add(null to "Racine")
        }
    }

    val currentParentId = breadcrumb.lastOrNull()?.first

    // Get current level folders
    val currentFolders = allFolders.filter { folder ->
        if (currentParentId == null) {
            folder.parentFolderId == null
        } else {
            folder.parentFolderId == currentParentId
        }
    }

    // Auto-select current parent folder when navigating into it
    LaunchedEffect(currentParentId) {
        if (currentParentId != null) {
            selectedFolderId = currentParentId
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Column {
                Text("Choisir un dossier", color = DarkTextPrimary, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                // Breadcrumb
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    breadcrumb.forEachIndexed { index, (folderId, name) ->
                        if (index > 0) {
                            Text(" > ", color = DarkTextMuted, fontSize = 12.sp)
                        }
                        Text(
                            text = name,
                            color = if (index == breadcrumb.lastIndex) Gold else DarkTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (index == breadcrumb.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.clickable {
                                // Navigate back to this level
                                while (breadcrumb.size > index + 1) {
                                    breadcrumb.removeAt(breadcrumb.lastIndex)
                                }
                                selectedFolderId = folderId
                            },
                        )
                    }
                }
            }
        },
        text = {
            if (currentFolders.isEmpty()) {
                Text(
                    text = "Aucun sous-dossier",
                    color = DarkTextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(250.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    gridItems(currentFolders) { folder ->
                        val isSelected = selectedFolderId == folder.id
                        Card(
                            modifier = Modifier
                                .clickable {
                                    coroutineScope.launch {
                                        val hasSubs = viewModel.hasSubFolders(folder.id)
                                        if (hasSubs) {
                                            breadcrumb.add(folder.id to folder.name)
                                            selectedFolderId = folder.id
                                        } else {
                                            selectedFolderId = folder.id
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) GoldDeep else DarkSurface,
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = if (isSelected) BorderStroke(1.dp, Gold) else BorderStroke(1.dp, Gold.copy(alpha = 0.1f)),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = folder.icon ?: "📁",
                                    fontSize = 24.sp,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = folder.name,
                                    color = DarkTextPrimary,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedFolderId?.let { onFolderSelected(it) }
                },
                enabled = selectedFolderId != null,
            ) {
                val selectedName = allFolders.find { it.id == selectedFolderId }?.name ?: ""
                Text(
                    text = if (selectedName.isNotEmpty()) "Choisir \"$selectedName\"" else "Choisir",
                    color = if (selectedFolderId != null) Gold else DarkTextMuted,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = DarkTextSecondary)
            }
        },
    )
}

// --- Section Header ---

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        color = DarkTextPrimary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
    )
}

// --- Folder Row / Grid with "Nouveau" card ---

@Composable
private fun FolderRow(
    folders: List<FolderEntity>,
    onFolderClick: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onRenameFolder: (FolderEntity, String) -> Unit,
    onCreateFolder: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // "Nouveau dossier" card as first item
        item {
            NewFolderCard(onClick = onCreateFolder)
        }
        items(folders) { folder ->
            FolderCard(
                folder = folder,
                onClick = { onFolderClick(folder) },
                onDelete = { onDeleteFolder(folder) },
                onRename = { newName -> onRenameFolder(folder, newName) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderGrid(
    folders: List<FolderEntity>,
    onFolderClick: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onRenameFolder: (FolderEntity, String) -> Unit,
    onCreateFolder: () -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // "Nouveau dossier" card as first item
        NewFolderCard(onClick = onCreateFolder)
        folders.forEach { folder ->
            FolderCard(
                folder = folder,
                onClick = { onFolderClick(folder) },
                onDelete = { onDeleteFolder(folder) },
                onRename = { newName -> onRenameFolder(folder, newName) },
            )
        }
    }
}

@Composable
private fun NewFolderCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GoldDeep.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Nouveau dossier",
                    tint = Gold,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nouveau",
                color = Gold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// --- Folder Card ---

@Composable
private fun FolderCard(
    folder: FolderEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = folder.icon ?: "📁",
                    fontSize = 32.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Options",
                            tint = DarkTextMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Renommer") },
                            onClick = {
                                showMenu = false
                                showRenameDialog = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = Color(0xFFCF6679)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = folder.name,
                color = DarkTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (showRenameDialog) {
        RenameFolderDialog(
            currentName = folder.name,
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                onRename(newName)
                showRenameDialog = false
            },
        )
    }
}

@Composable
private fun RenameFolderDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = { Text("Renommer le dossier", color = DarkTextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = DarkTextMuted,
                    focusedLabelColor = Gold,
                    unfocusedLabelColor = DarkTextMuted,
                    focusedTextColor = DarkTextPrimary,
                    unfocusedTextColor = DarkTextPrimary,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onRename(name.trim()) }) {
                Text("Renommer", color = Gold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = DarkTextSecondary)
            }
        },
    )
}

@Composable
private fun EmptyRecentState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOpen,
            contentDescription = null,
            tint = DarkTextMuted,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Aucun fichier récent",
            color = DarkTextSecondary,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun RecentFileItem(
    document: PdfDocumentEntity,
    onClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.title,
                color = DarkTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${document.pageCount} pages",
                color = DarkTextMuted,
                fontSize = 12.sp,
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Options",
                    tint = DarkTextMuted,
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Ouvrir") },
                    onClick = {
                        showMenu = false
                        onClick()
                    },
                )
            }
        }
    }
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, icon: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<String?>("📁") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = { Text("Nouveau dossier", color = DarkTextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du dossier") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = DarkTextMuted,
                        focusedLabelColor = Gold,
                        unfocusedLabelColor = DarkTextMuted,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Icône",
                    color = DarkTextSecondary,
                    fontSize = 13.sp,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Icon picker grid
                val chunkedIcons = folderIconOptions.chunked(4)
                chunkedIcons.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { icon ->
                            val isSelected = selectedIcon == icon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) GoldDeep else DarkSurface,
                                    )
                                    .clickable { selectedIcon = icon },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = icon, fontSize = 22.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim(), selectedIcon) },
            ) {
                Text("Créer", color = Gold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = DarkTextSecondary)
            }
        },
    )
}
