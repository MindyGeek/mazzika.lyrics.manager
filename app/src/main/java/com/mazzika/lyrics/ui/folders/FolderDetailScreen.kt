package com.mazzika.lyrics.ui.folders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.ui.home.CreateFolderDialog
import com.mazzika.lyrics.ui.theme.DarkBackground
import com.mazzika.lyrics.ui.theme.DarkSurface
import com.mazzika.lyrics.ui.theme.DarkSurfaceElevated
import com.mazzika.lyrics.ui.theme.DarkTextMuted
import com.mazzika.lyrics.ui.theme.DarkTextPrimary
import com.mazzika.lyrics.ui.theme.DarkTextSecondary
import com.mazzika.lyrics.ui.theme.Gold

// Gold glow gradient for file item icon
private val GoldGlowGradient = Brush.linearGradient(
    listOf(Color(0x26C5A028), Color(0x14C5A028))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFolder: (Long) -> Unit,
    onNavigateToReader: (Long) -> Unit,
    viewModel: FolderDetailViewModel = viewModel(),
) {
    val folder by viewModel.folder.collectAsState()
    val subFolders by viewModel.subFolders.collectAsState()
    val documents by viewModel.documents.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCatalogCopyDialog by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importFileToFolder(uri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
    ) {
        if (subFolders.isEmpty() && documents.isEmpty()) {
            EmptyFolderState(
                modifier = Modifier.fillMaxSize(),
                onCreateSubFolder = { showCreateFolderDialog = true },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            ) {
                // Sub-folders section
                if (subFolders.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Sous-dossiers",
                            count = subFolders.size,
                        )
                    }
                    item {
                        SubFolderRow(
                            folders = subFolders,
                            onFolderClick = { onNavigateToFolder(it.id) },
                            onDeleteFolder = { viewModel.deleteSubFolder(it.id) },
                            onRenameFolder = { folder, newName -> viewModel.renameSubFolder(folder.id, newName) },
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Documents section
                if (documents.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Fichiers",
                            count = documents.size,
                        )
                    }
                    items(documents) { document ->
                        FolderDocumentItem(
                            document = document,
                            onClick = { onNavigateToReader(document.id) },
                            onRemove = { viewModel.removeDocumentFromFolder(document.id) },
                        )
                    }
                }
            }
        }

        // Dim overlay when FAB is expanded
        if (fabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { fabExpanded = false },
            )
        }

        // Expandable FAB menu
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Speed dial options
            AnimatedVisibility(
                visible = fabExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SpeedDialItem(
                        label = "Copier depuis le catalogue",
                        icon = Icons.Filled.ContentCopy,
                        onClick = {
                            fabExpanded = false
                            showCatalogCopyDialog = true
                        },
                    )
                    SpeedDialItem(
                        label = "Importer fichier",
                        icon = Icons.Filled.UploadFile,
                        onClick = {
                            fabExpanded = false
                            importFileLauncher.launch(arrayOf("application/pdf"))
                        },
                    )
                    SpeedDialItem(
                        label = "Nouveau dossier",
                        icon = Icons.Filled.CreateNewFolder,
                        onClick = {
                            fabExpanded = false
                            showCreateFolderDialog = true
                        },
                    )
                }
            }

            // Main FAB
            FloatingActionButton(
                onClick = { fabExpanded = !fabExpanded },
                containerColor = Gold,
                contentColor = DarkBackground,
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = if (fabExpanded) Icons.Filled.Close else Icons.Filled.Add,
                    contentDescription = if (fabExpanded) "Fermer le menu" else "Options",
                )
            }
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { name, icon ->
                viewModel.createSubFolder(name, icon)
                showCreateFolderDialog = false
            },
        )
    }

    if (showCatalogCopyDialog) {
        CatalogCopyDialog(
            viewModel = viewModel,
            onDismiss = { showCatalogCopyDialog = false },
        )
    }
}

@Composable
private fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = label,
                color = DarkTextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = DarkSurfaceElevated,
            contentColor = Gold,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CatalogCopyDialog(
    viewModel: FolderDetailViewModel,
    onDismiss: () -> Unit,
) {
    val allDocs by viewModel.allCatalogDocuments.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = { Text("Copier depuis le catalogue", color = DarkTextPrimary) },
        text = {
            if (allDocs.isEmpty()) {
                Text(
                    text = "Aucun document dans le catalogue",
                    color = DarkTextMuted,
                    fontSize = 14.sp,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                ) {
                    items(allDocs) { doc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addDocumentToFolder(doc.id)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = Gold,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.title,
                                    color = DarkTextPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${doc.pageCount} page${if (doc.pageCount != 1) "s" else ""}",
                                    color = DarkTextMuted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = DarkTextSecondary)
            }
        },
    )
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int = 0,
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = DarkTextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        if (count > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "$count",
                    color = DarkTextMuted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SubFolderRow(
    folders: List<FolderEntity>,
    onFolderClick: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onRenameFolder: (FolderEntity, String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(folders) { folder ->
            SubFolderCard(
                folder = folder,
                onClick = { onFolderClick(folder) },
                onDelete = { onDeleteFolder(folder) },
                onRename = { newName -> onRenameFolder(folder, newName) },
            )
        }
    }
}

@Composable
private fun SubFolderCard(
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
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = folder.icon ?: "\uD83D\uDCC1",
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
                        containerColor = DarkSurfaceElevated,
                    ) {
                        DropdownMenuItem(
                            text = { Text("Renommer", color = DarkTextPrimary) },
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
        RenameSubFolderDialog(
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
private fun RenameSubFolderDialog(
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
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom") },
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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
private fun FolderDocumentItem(
    document: PdfDocumentEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurface)
                .clickable { onClick() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Gold glow icon box with emoji
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(GoldGlowGradient),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\uD83C\uDFB5", // 🎵
                    fontSize = 20.sp,
                    color = Gold,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

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
                    text = "${document.pageCount} page${if (document.pageCount != 1) "s" else ""}",
                    color = DarkTextMuted,
                    fontSize = 11.5.sp,
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
                    containerColor = DarkSurfaceElevated,
                ) {
                    DropdownMenuItem(
                        text = { Text("Ouvrir", color = DarkTextPrimary) },
                        onClick = {
                            showMenu = false
                            onClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Retirer du dossier", color = Color(0xFFCF6679)) },
                        onClick = {
                            showMenu = false
                            onRemove()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFolderState(
    modifier: Modifier = Modifier,
    onCreateSubFolder: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOpen,
            contentDescription = null,
            tint = DarkTextMuted,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Dossier vide",
            color = DarkTextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Appuyez sur + pour ajouter du contenu",
            color = DarkTextMuted,
            fontSize = 13.sp,
        )
    }
}
