package com.mazzika.lyrics.ui.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.ui.home.CreateFolderDialog
import com.mazzika.lyrics.ui.home.HomeViewModel
import com.mazzika.lyrics.ui.theme.DarkBackground
import com.mazzika.lyrics.ui.theme.DarkSurface
import com.mazzika.lyrics.ui.theme.DarkSurfaceElevated
import com.mazzika.lyrics.ui.theme.DarkTextMuted
import com.mazzika.lyrics.ui.theme.DarkTextPrimary
import com.mazzika.lyrics.ui.theme.DarkTextSecondary
import com.mazzika.lyrics.ui.theme.Gold

@Composable
fun FoldersScreen(
    onNavigateToFolder: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val rootFolders by viewModel.rootFolders.collectAsState()
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                NewFolderCard(onClick = { showCreateFolderDialog = true })
            }
            items(rootFolders) { folder ->
                FolderCard(
                    folder = folder,
                    onClick = { onNavigateToFolder(folder.id) },
                    onDelete = { viewModel.deleteFolder(folder.id) },
                    onRename = { newName -> viewModel.renameFolder(folder.id, newName) },
                )
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
private fun NewFolderCard(onClick: () -> Unit) {
    val dashedBorderColor = Color(0x40C5A028)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .drawBehind {
                val stroke = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                        0f,
                    ),
                )
                drawRoundRect(
                    color = dashedBorderColor,
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = stroke,
                    size = Size(size.width, size.height),
                )
            }
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "+",
                fontSize = 22.sp,
                color = DarkTextPrimary.copy(alpha = 0.4f),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Nouveau",
                color = Gold.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurface)
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = folder.icon ?: "\uD83D\uDCC1",
                    fontSize = 28.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Options",
                            tint = DarkTextMuted,
                            modifier = Modifier.size(14.dp),
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
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
