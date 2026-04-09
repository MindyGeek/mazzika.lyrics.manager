package com.mazzika.lyrics.ui.catalog

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CatalogScreen(
    onNavigateToReader: (Long) -> Unit,
    viewModel: CatalogViewModel = viewModel(),
) {
    val documents by viewModel.documents.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val allFolders by viewModel.allFolders.collectAsState()

    var documentToAddToFolder by remember { mutableStateOf<Long?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.importPdf(it) }
    }

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            // Header
            item {
                CatalogHeader(documentCount = documents.size)
            }

            // Search bar
            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                )
            }

            // Sort chips
            item {
                SortChips(
                    currentMode = sortMode,
                    onModeChange = { viewModel.setSortMode(it) },
                )
            }

            // File list or empty state
            if (documents.isEmpty()) {
                item {
                    EmptyCatalogState()
                }
            } else if (isTablet) {
                item {
                    TwoColumnFileGrid(
                        documents = documents,
                        onNavigateToReader = onNavigateToReader,
                        onDelete = { viewModel.deleteDocument(it) },
                        onAddToFolder = { documentToAddToFolder = it },
                    )
                }
            } else {
                items(documents) { document ->
                    FileCard(
                        document = document,
                        onClick = { onNavigateToReader(document.id) },
                        onDelete = { viewModel.deleteDocument(document.id) },
                        onAddToFolder = { documentToAddToFolder = document.id },
                    )
                }
            }
        }

        // FAB Import
        FloatingActionButton(
            onClick = {
                if (!isImporting) {
                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                }
            },
            containerColor = Gold,
            contentColor = DarkBackground,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            if (isImporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = DarkBackground,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Importer un PDF")
            }
        }
    }

    // Dialog: Ajouter à un dossier
    documentToAddToFolder?.let { docId ->
        AddToFolderDialog(
            folders = allFolders,
            onDismiss = { documentToAddToFolder = null },
            onFolderSelected = { folderId ->
                viewModel.addToFolder(docId, folderId)
                documentToAddToFolder = null
            },
        )
    }
}

@Composable
private fun CatalogHeader(documentCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Catalogue",
                fontFamily = PlayfairDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Gold,
            )
            Text(
                text = "$documentCount fichier${if (documentCount != 1) "s" else ""}",
                fontSize = 13.sp,
                color = DarkTextMuted,
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        placeholder = { Text("Rechercher...", color = DarkTextMuted) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Rechercher",
                tint = DarkTextMuted,
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            unfocusedBorderColor = DarkSurface,
            focusedTextColor = DarkTextPrimary,
            unfocusedTextColor = DarkTextPrimary,
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface,
            cursorColor = Gold,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortChips(
    currentMode: SortMode,
    onModeChange: (SortMode) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        item {
            FilterChip(
                selected = currentMode == SortMode.RECENT,
                onClick = { onModeChange(SortMode.RECENT) },
                label = { Text("Récents", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = DarkSurface,
                    selectedContainerColor = GoldDeep,
                    labelColor = DarkTextSecondary,
                    selectedLabelColor = Gold,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = currentMode == SortMode.RECENT,
                    borderColor = DarkSurface,
                    selectedBorderColor = Gold,
                ),
            )
        }
        item {
            FilterChip(
                selected = currentMode == SortMode.TITLE,
                onClick = { onModeChange(SortMode.TITLE) },
                label = { Text("A-Z", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = DarkSurface,
                    selectedContainerColor = GoldDeep,
                    labelColor = DarkTextSecondary,
                    selectedLabelColor = Gold,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = currentMode == SortMode.TITLE,
                    borderColor = DarkSurface,
                    selectedBorderColor = Gold,
                ),
            )
        }
    }
}

@Composable
private fun TwoColumnFileGrid(
    documents: List<PdfDocumentEntity>,
    onNavigateToReader: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onAddToFolder: (Long) -> Unit,
) {
    val rows = documents.chunked(2)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { rowDocs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowDocs.forEach { document ->
                    Box(modifier = Modifier.weight(1f)) {
                        FileCard(
                            document = document,
                            onClick = { onNavigateToReader(document.id) },
                            onDelete = { onDelete(document.id) },
                            onAddToFolder = { onAddToFolder(document.id) },
                            isGridItem = true,
                        )
                    }
                }
                // Fill empty cell if odd count
                if (rowDocs.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FileCard(
    document: PdfDocumentEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onAddToFolder: () -> Unit,
    isGridItem: Boolean = false,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isGridItem) 0.dp else 20.dp,
                vertical = if (isGridItem) 0.dp else 6.dp,
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail
            DocumentThumbnail(thumbnailPath = document.thumbnailPath)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    color = DarkTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${document.pageCount} page${if (document.pageCount != 1) "s" else ""}",
                    color = DarkTextMuted,
                    fontSize = 12.sp,
                )
                Text(
                    text = formatDate(document.importedAt),
                    color = DarkTextMuted,
                    fontSize = 11.sp,
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
                        text = { Text("Ajouter à un dossier", color = DarkTextPrimary) },
                        onClick = {
                            showMenu = false
                            onAddToFolder()
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
    }
}

@Composable
private fun AddToFolderDialog(
    folders: List<FolderEntity>,
    onDismiss: () -> Unit,
    onFolderSelected: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = { Text("Ajouter à un dossier", color = DarkTextPrimary) },
        text = {
            if (folders.isEmpty()) {
                Text("Aucun dossier. Créez-en un depuis l'accueil.", color = DarkTextMuted, fontSize = 14.sp)
            } else {
                Column {
                    folders.forEach { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFolderSelected(folder.id) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = folder.icon ?: "\uD83D\uDCC1",
                                fontSize = 20.sp,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = folder.name,
                                color = DarkTextPrimary,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
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

@Composable
private fun DocumentThumbnail(thumbnailPath: String) {
    val bitmap = remember(thumbnailPath) {
        runCatching {
            val file = File(thumbnailPath)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }.getOrNull()
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun EmptyCatalogState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOpen,
            contentDescription = null,
            tint = DarkTextMuted,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Aucun fichier",
            color = DarkTextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Appuyez sur + pour importer un PDF",
            color = DarkTextMuted,
            fontSize = 13.sp,
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
