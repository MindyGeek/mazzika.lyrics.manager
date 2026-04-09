package com.mazzika.lyrics.ui.catalog

// thumbnail imports removed
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.ui.home.FolderSelectorDialogContent
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.ui.theme.DarkBackground
import com.mazzika.lyrics.ui.theme.DarkSurface
import com.mazzika.lyrics.ui.theme.DarkSurfaceElevated
import com.mazzika.lyrics.ui.theme.DarkTextMuted
import com.mazzika.lyrics.ui.theme.DarkTextPrimary
import com.mazzika.lyrics.ui.theme.DarkTextSecondary
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.GoldLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Gold glow gradient for file item icon
private val GoldGlowGradient = Brush.linearGradient(
    listOf(Color(0x26C5A028), Color(0x14C5A028))
)

// Gold gradient for active chip
private val GoldChipGradient = Brush.linearGradient(
    listOf(Gold, GoldLight)
)

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

    // Dialog: Ajouter a un dossier
    documentToAddToFolder?.let { docId ->
        FolderSelectorDialogContent(
            allFolders = allFolders,
            hasSubFolders = { folderId -> viewModel.hasSubFolders(folderId) },
            onDismiss = { documentToAddToFolder = null },
            onFolderSelected = { folderId ->
                viewModel.addToFolder(docId, folderId)
                documentToAddToFolder = null
            },
        )
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Rechercher...", color = DarkTextMuted) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Rechercher",
                tint = DarkTextMuted,
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = DarkTextPrimary,
            unfocusedTextColor = DarkTextPrimary,
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface,
            cursorColor = Gold,
        ),
    )
}

@Composable
private fun SortChips(
    currentMode: SortMode,
    onModeChange: (SortMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SortChipItem(
            label = "R\u00E9cents",
            selected = currentMode == SortMode.RECENT,
            onClick = { onModeChange(SortMode.RECENT) },
        )
        SortChipItem(
            label = "A-Z",
            selected = currentMode == SortMode.TITLE,
            onClick = { onModeChange(SortMode.TITLE) },
        )
    }
}

@Composable
private fun SortChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (selected) {
                    Modifier.background(GoldChipGradient)
                } else {
                    Modifier.background(DarkSurface)
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color(0xFF0A0800) else DarkTextSecondary,
        )
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
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isGridItem) 0.dp else 16.dp,
                vertical = if (isGridItem) 0.dp else 3.dp,
            ),
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
            // Gold glow icon box with 🎵 emoji
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${document.pageCount} page${if (document.pageCount != 1) "s" else ""} \u00B7 ${formatDate(document.importedAt)}",
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
                        text = { Text("Ajouter \u00E0 un dossier", color = DarkTextPrimary) },
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
private fun EmptyCatalogState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 48.dp),
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
