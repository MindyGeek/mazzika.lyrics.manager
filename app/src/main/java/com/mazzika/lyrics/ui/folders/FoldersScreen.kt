package com.mazzika.lyrics.ui.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.ui.components.FolderCard
import com.mazzika.lyrics.ui.components.FolderNewCard
import com.mazzika.lyrics.ui.home.CreateFolderDialog
import com.mazzika.lyrics.ui.home.HomeViewModel
import com.mazzika.lyrics.ui.theme.Inter
import com.mazzika.lyrics.ui.theme.LocalStudioTokens

@Composable
fun FoldersScreen(
    onNavigateToFolder: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val tokens = LocalStudioTokens.current
    val rootFolders by viewModel.rootFolders.collectAsState()
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(tokens.bg)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(currentLineSpan = 3) }) {
                FoldersTitle(count = rootFolders.size)
            }

            item {
                FolderNewCard(onClick = { showCreateFolderDialog = true })
            }

            items(rootFolders) { folder ->
                InteractiveFolderCard(
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

@Suppress("FunctionName")
private fun GridItemSpan(currentLineSpan: Int) = androidx.compose.foundation.lazy.grid.GridItemSpan(currentLineSpan)

@Composable
private fun FoldersTitle(count: Int) {
    val tokens = LocalStudioTokens.current
    Row(
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "Mes Dossiers",
            fontFamily = Inter,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            letterSpacing = (-0.72).sp,
            color = tokens.text,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$count dossier${if (count > 1) "s" else ""}",
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = tokens.textMid,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun InteractiveFolderCard(
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
        FolderCard(
            emoji = folder.icon ?: "📁",
            name = folder.name,
            count = null,
            onClick = onClick,
        )
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Renommer", fontFamily = Inter) },
                    leadingIcon = { Icon(Icons.Filled.Edit, null, Modifier.size(18.dp), tint = tokens.textMid) },
                    onClick = { showMenu = false; showRenameDialog = true },
                )
                DropdownMenuItem(
                    text = { Text("Supprimer", color = tokens.danger, fontFamily = Inter) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, Modifier.size(18.dp), tint = tokens.danger) },
                    onClick = { showMenu = false; onDelete() },
                )
            }
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
fun RenameFolderDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    val tokens = LocalStudioTokens.current
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.surface,
        title = {
            Text(
                "Renommer le dossier",
                color = tokens.text,
                fontFamily = Inter,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom", fontFamily = Inter) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tokens.gold,
                    unfocusedBorderColor = tokens.border,
                    focusedLabelColor = tokens.gold,
                    unfocusedLabelColor = tokens.textMid,
                    focusedTextColor = tokens.text,
                    unfocusedTextColor = tokens.text,
                    cursorColor = tokens.gold,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onRename(name.trim()) }) {
                Text("Renommer", color = tokens.gold, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = tokens.textMid, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}
