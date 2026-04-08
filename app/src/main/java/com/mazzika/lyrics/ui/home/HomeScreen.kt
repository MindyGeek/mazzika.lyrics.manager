package com.mazzika.lyrics.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.mazzika.lyrics.ui.theme.PlayfairDisplay

private val folderIconOptions = listOf("📁", "🎶", "🎸", "⭐", "🎼", "✨", "📦", "🎤")

@Composable
fun HomeScreen(
    onNavigateToFolder: (Long) -> Unit,
    onNavigateToReader: (Long) -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val rootFolders by viewModel.rootFolders.collectAsState()
    val recentDocuments by viewModel.recentDocuments.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateFolderDialog = true },
                containerColor = Gold,
                contentColor = DarkBackground,
                shape = CircleShape,
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Créer un dossier")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            // Header
            item {
                HomeHeader(
                    onNavigateToSync = onNavigateToSync,
                    onNavigateToSettings = onNavigateToSettings,
                )
            }

            // Quick action chips
            item {
                QuickActionChips(
                    onCreateSession = { /* TODO */ },
                    onJoin = onNavigateToSync,
                    onImport = { /* TODO */ },
                )
            }

            // Folders section
            item {
                SectionHeader(title = "Mes Dossiers")
            }

            item {
                if (rootFolders.isEmpty()) {
                    EmptyFoldersState(
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
}

@Composable
private fun HomeHeader(
    onNavigateToSync: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Mazzika",
                fontFamily = PlayfairDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = Gold,
            )
            Text(
                text = "BAND LYRICS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTextMuted,
                letterSpacing = 3.sp,
            )
        }

        Row {
            IconButton(onClick = onNavigateToSync) {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = "Synchronisation",
                    tint = DarkTextSecondary,
                )
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Réglages",
                    tint = DarkTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun QuickActionChips(
    onCreateSession: () -> Unit,
    onJoin: () -> Unit,
    onImport: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            QuickChip(label = "Créer session", onClick = onCreateSession)
        }
        item {
            QuickChip(label = "Rejoindre", onClick = onJoin)
        }
        item {
            QuickChip(label = "Importer", onClick = onImport)
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickChip(label: String, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(text = label, color = DarkTextPrimary, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = DarkSurface,
            selectedContainerColor = GoldDeep,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = false,
            borderColor = GoldDeep,
            selectedBorderColor = Gold,
        ),
    )
}

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

@Composable
private fun FolderRow(
    folders: List<FolderEntity>,
    onFolderClick: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onRenameFolder: (FolderEntity, String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
private fun EmptyFoldersState(onCreateFolder: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.CreateNewFolder,
            contentDescription = null,
            tint = DarkTextMuted,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Aucun dossier",
            color = DarkTextSecondary,
            fontSize = 14.sp,
        )
        TextButton(onClick = onCreateFolder) {
            Text("Créer un dossier", color = Gold)
        }
    }
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
