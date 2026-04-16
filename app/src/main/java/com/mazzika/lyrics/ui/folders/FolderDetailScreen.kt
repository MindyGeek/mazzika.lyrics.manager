package com.mazzika.lyrics.ui.folders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.ui.components.DocRow
import com.mazzika.lyrics.ui.components.GroupLabel
import com.mazzika.lyrics.ui.components.SectionHeader
import com.mazzika.lyrics.ui.components.StudioFab
import com.mazzika.lyrics.ui.components.SubfolderChip
import com.mazzika.lyrics.ui.components.paletteFor
import com.mazzika.lyrics.ui.home.CreateFolderDialog
import com.mazzika.lyrics.ui.theme.Inter
import com.mazzika.lyrics.ui.theme.LocalStudioTokens

@Composable
fun FolderDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFolder: (Long) -> Unit,
    onNavigateToReader: (Long) -> Unit,
    viewModel: FolderDetailViewModel = viewModel(),
) {
    val tokens = LocalStudioTokens.current
    val folder by viewModel.folder.collectAsState()
    val subFolders by viewModel.subFolders.collectAsState()
    val documents by viewModel.documents.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCatalogCopyDialog by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.importFileToFolder(uri) }

    Box(modifier = Modifier.fillMaxSize().background(tokens.bg)) {
        if (subFolders.isEmpty() && documents.isEmpty()) {
            EmptyFolderState(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
            ) {
                // Sub-folders
                if (subFolders.isNotEmpty()) {
                    item {
                        GroupLabel(
                            text = "Sous-dossiers",
                            withDot = false,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 0.dp),
                        )
                    }
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(subFolders) { sub ->
                                InteractiveSubfolderChip(
                                    folder = sub,
                                    onClick = { onNavigateToFolder(sub.id) },
                                    onDelete = { viewModel.deleteSubFolder(sub.id) },
                                    onRename = { newName -> viewModel.renameSubFolder(sub.id, newName) },
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }

                // Documents
                if (documents.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp)) {
                            SectionHeader(title = "Fichiers", count = documents.size)
                        }
                    }
                    items(documents) { doc ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            InteractiveDocRow(
                                document = doc,
                                onClick = { onNavigateToReader(doc.id) },
                                onRemove = { viewModel.removeDocumentFromFolder(doc.id) },
                            )
                        }
                    }
                }
            }
        }

        // Dim overlay when FAB expanded
        if (fabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { fabExpanded = false },
            )
        }

        // Speed-dial FAB
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(
                visible = fabExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SpeedDialItem("Copier depuis le catalogue", Icons.Filled.ContentCopy) {
                        fabExpanded = false; showCatalogCopyDialog = true
                    }
                    SpeedDialItem("Importer fichier", Icons.Filled.UploadFile) {
                        fabExpanded = false; importFileLauncher.launch(arrayOf("application/pdf"))
                    }
                    SpeedDialItem("Nouveau sous-dossier", Icons.Filled.CreateNewFolder) {
                        fabExpanded = false; showCreateFolderDialog = true
                    }
                }
            }

            StudioFab(onClick = { fabExpanded = !fabExpanded }) {
                Icon(
                    imageVector = if (fabExpanded) Icons.Filled.Close else Icons.Filled.Add,
                    contentDescription = if (fabExpanded) "Fermer" else "Ouvrir",
                    modifier = Modifier.size(28.dp),
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
        CatalogCopyDialog(viewModel = viewModel, onDismiss = { showCatalogCopyDialog = false })
    }
}

@Composable
private fun SpeedDialItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    val tokens = LocalStudioTokens.current
    // Whole row triggers onClick — label and icon are both valid tap targets.
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (tokens.isDark) tokens.surfaceHi else tokens.surface)
                .border(1.dp, tokens.cardBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = label,
                color = tokens.text,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (tokens.isDark) tokens.surfaceHi else tokens.surface)
                .border(1.dp, tokens.cardBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tokens.gold,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InteractiveSubfolderChip(
    folder: FolderEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
) {
    val tokens = LocalStudioTokens.current
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = { showMenu = true },
        ),
    ) {
        SubfolderChip(
            emoji = folder.icon ?: "📁",
            name = folder.name,
            count = "",
            onClick = onClick,
        )
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Renommer", fontFamily = Inter) },
                onClick = { showMenu = false; showRenameDialog = true },
            )
            DropdownMenuItem(
                text = { Text("Supprimer", color = tokens.danger, fontFamily = Inter) },
                onClick = { showMenu = false; onDelete() },
            )
        }
    }

    if (showRenameDialog) {
        RenameFolderDialog(
            currentName = folder.name,
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                onRename(newName); showRenameDialog = false
            },
        )
    }
}

@Composable
private fun InteractiveDocRow(
    document: PdfDocumentEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val tokens = LocalStudioTokens.current
    var showMenu by remember { mutableStateOf(false) }
    Box {
        DocRow(
            title = document.title,
            letter = document.title.firstOrNull()?.uppercase() ?: "?",
            palette = paletteFor(document.id),
            meta = "${document.pageCount} pages",
            onClick = onClick,
            onMoreClick = { showMenu = true },
        )
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            DropdownMenuItem(
                text = { Text("Ouvrir", fontFamily = Inter) },
                onClick = { showMenu = false; onClick() },
            )
            DropdownMenuItem(
                text = { Text("Retirer du dossier", color = tokens.danger, fontFamily = Inter) },
                onClick = { showMenu = false; onRemove() },
            )
        }
    }
}

@Composable
private fun CatalogCopyDialog(
    viewModel: FolderDetailViewModel,
    onDismiss: () -> Unit,
) {
    val tokens = LocalStudioTokens.current
    val allDocs by viewModel.allCatalogDocuments.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.surface,
        title = {
            Text(
                "Copier depuis le catalogue",
                color = tokens.text,
                fontFamily = Inter,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
            )
        },
        text = {
            if (allDocs.isEmpty()) {
                Text(
                    "Aucun document dans le catalogue",
                    color = tokens.textMid,
                    fontFamily = Inter,
                    fontSize = 14.sp,
                )
            } else {
                LazyColumn(modifier = Modifier.height(300.dp)) {
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
                                tint = tokens.gold,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.title,
                                    color = tokens.text,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${doc.pageCount} page${if (doc.pageCount != 1) "s" else ""}",
                                    color = tokens.textMid,
                                    fontFamily = Inter,
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
                Text("Fermer", color = tokens.textMid, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

@Composable
private fun EmptyFolderState(modifier: Modifier = Modifier) {
    val tokens = LocalStudioTokens.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "📂", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Dossier vide",
            color = tokens.textMid,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Appuyez sur + pour ajouter du contenu",
            color = tokens.textDim,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}
