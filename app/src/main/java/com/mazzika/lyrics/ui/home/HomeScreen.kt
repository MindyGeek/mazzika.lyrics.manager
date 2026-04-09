package com.mazzika.lyrics.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.ui.theme.Amber
import com.mazzika.lyrics.ui.theme.DarkBackground
import com.mazzika.lyrics.ui.theme.DarkSurface
import com.mazzika.lyrics.ui.theme.DarkSurfaceElevated
import com.mazzika.lyrics.ui.theme.DarkTextMuted
import com.mazzika.lyrics.ui.theme.DarkTextPrimary
import com.mazzika.lyrics.ui.theme.DarkTextSecondary
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.GoldDeep
import kotlinx.coroutines.launch

private val folderIconOptions = listOf("\uD83D\uDCC1", "\uD83C\uDFB6", "\uD83C\uDFB8", "\u2B50", "\uD83C\uDFBC", "\u2728", "\uD83D\uDCE6", "\uD83C\uDFA4")

// Action card color definitions
private val TealGradientBg = Brush.verticalGradient(
    listOf(Color(0x1F5DB8A9), Color(0x0A5DB8A9))
)
private val TealIconGradient = Brush.linearGradient(
    listOf(Color(0xFF5DB8A9), Color(0xFF3D9B8F))
)
private val BlueGradientBg = Brush.verticalGradient(
    listOf(Color(0x1F6BA3D4), Color(0x0A6BA3D4))
)
private val BlueIconGradient = Brush.linearGradient(
    listOf(Color(0xFF6BA3D4), Color(0xFF4A82B8))
)
private val AmberGradientBg = Brush.verticalGradient(
    listOf(Color(0x1FD4A054), Color(0x0AD4A054))
)
private val AmberIconGradient = Brush.linearGradient(
    listOf(Color(0xFFD4A054), Color(0xFFB88A3E))
)

// Gold glow gradient for file item icon
private val GoldGlowGradient = Brush.linearGradient(
    listOf(Color(0x26C5A028), Color(0x14C5A028))
)

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
                SectionHeader(
                    title = "Mes Dossiers",
                    count = rootFolders.size,
                )
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
                SectionHeader(
                    title = "R\u00E9cemment ouverts",
                    count = recentDocuments.size,
                    showSeeAll = recentDocuments.size > 3,
                )
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
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActionCard(
            emoji = "\uD83D\uDCE1", // 📡
            title = "Cr\u00E9er session",
            backgroundBrush = TealGradientBg,
            iconBrush = TealIconGradient,
            onClick = onCreateSession,
            modifier = Modifier.weight(1f),
        )
        ActionCard(
            emoji = "\uD83D\uDD0D", // 🔍
            title = "Rejoindre",
            backgroundBrush = BlueGradientBg,
            iconBrush = BlueIconGradient,
            onClick = onJoin,
            modifier = Modifier.weight(1f),
        )
        ActionCard(
            emoji = "\uD83D\uDCE5", // 📥
            title = "Importer",
            backgroundBrush = AmberGradientBg,
            iconBrush = AmberIconGradient,
            onClick = onImport,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionCard(
    emoji: String,
    title: String,
    backgroundBrush: Brush,
    iconBrush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                color = Color(0x0AFFFFFF),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Icon container: 44dp rounded square with gradient
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBrush),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emoji,
                    fontSize = 20.sp,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = DarkTextPrimary,
                fontSize = 11.5.sp,
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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

    val breadcrumb = remember { mutableStateListOf<Pair<Long?, String>>() }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        if (breadcrumb.isEmpty()) {
            breadcrumb.add(null to "Racine")
        }
    }

    val currentParentId = breadcrumb.lastOrNull()?.first

    val currentFolders = allFolders.filter { folder ->
        if (currentParentId == null) {
            folder.parentFolderId == null
        } else {
            folder.parentFolderId == currentParentId
        }
    }

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
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
                            border = if (isSelected) BorderStroke(1.dp, Gold) else null,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = folder.icon ?: "\uD83D\uDCC1",
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
private fun SectionHeader(
    title: String,
    count: Int = 0,
    showSeeAll: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
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
                    text = count.toString(),
                    color = DarkTextMuted,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (showSeeAll) {
            Text(
                text = "Voir tout",
                color = Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { /* TODO */ },
            )
        }
    }
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
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
    val dashedBorderColor = Color(0x40C5A028) // rgba(197,160,40,0.25)
    Box(
        modifier = Modifier
            .widthIn(min = 100.dp)
            .width(100.dp)
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

    Box(
        modifier = Modifier
            .widthIn(min = 100.dp)
            .width(100.dp)
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

@Composable
private fun EmptyRecentState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
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
            text = "Aucun fichier r\u00E9cent",
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp), // 6dp total between items (3dp top + 3dp bottom)
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
            // Gold glow icon box
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
                    text = "${document.pageCount} pages",
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
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, icon: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<String?>("\uD83D\uDCC1") }

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
                    text = "Ic\u00F4ne",
                    color = DarkTextSecondary,
                    fontSize = 13.sp,
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                Text("Cr\u00E9er", color = Gold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = DarkTextSecondary)
            }
        },
    )
}
