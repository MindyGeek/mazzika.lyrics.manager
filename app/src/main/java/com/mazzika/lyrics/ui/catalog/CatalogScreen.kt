package com.mazzika.lyrics.ui.catalog

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.ui.components.DocRow
import com.mazzika.lyrics.ui.components.FilterChip
import com.mazzika.lyrics.ui.components.SearchInputBar
import com.mazzika.lyrics.ui.components.StudioFab
import com.mazzika.lyrics.ui.components.paletteFor
import com.mazzika.lyrics.ui.home.FolderSelectorDialogContent
import com.mazzika.lyrics.ui.theme.Inter
import com.mazzika.lyrics.ui.theme.LocalStudioTokens

private enum class CatalogChip(val label: String) {
    RECENT("Récents"),
    A_Z("A-Z"),
    BY_FOLDER("Par dossier"),
    FAVORITES("Favoris"),
}

@Composable
fun CatalogScreen(
    onNavigateToReader: (Long) -> Unit,
    viewModel: CatalogViewModel = viewModel(),
) {
    val tokens = LocalStudioTokens.current
    val documents by viewModel.documents.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val allFolders by viewModel.allFolders.collectAsState()

    var documentToAddToFolder by remember { mutableStateOf<Long?>(null) }
    var selectedChip by remember { mutableStateOf(CatalogChip.RECENT) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.importPdf(it) } }

    Box(modifier = Modifier.fillMaxSize().background(tokens.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            // ── Title + count
            item { CatalogTitle(count = documents.size) }

            // ── Search
            item {
                Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                    SearchInputBar(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                    )
                }
            }

            // ── Filter chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(CatalogChip.entries) { chip ->
                        FilterChip(
                            label = chip.label,
                            selected = chip == selectedChip,
                            onClick = {
                                selectedChip = chip
                                when (chip) {
                                    CatalogChip.RECENT -> viewModel.setSortMode(SortMode.RECENT)
                                    CatalogChip.A_Z -> viewModel.setSortMode(SortMode.TITLE)
                                    else -> Unit
                                }
                            },
                        )
                    }
                }
            }

            // ── Documents list
            if (documents.isEmpty() && !isImporting) {
                item { EmptyCatalogState() }
            } else {
                items(documents) { doc ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        DocRowWithMenu(
                            document = doc,
                            onClick = { onNavigateToReader(doc.id) },
                            onAddToFolder = { documentToAddToFolder = doc.id },
                            onDelete = { viewModel.deleteDocument(doc.id) },
                        )
                    }
                }
            }
        }

        // ── Loading overlay
        if (isImporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = tokens.gold)
            }
        }

        // ── FAB
        StudioFab(
            onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Importer", modifier = Modifier.size(28.dp))
        }
    }

    // Add-to-folder dialog
    val docId = documentToAddToFolder
    if (docId != null) {
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
private fun CatalogTitle(count: Int) {
    val tokens = LocalStudioTokens.current
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "Catalogue",
            fontFamily = Inter,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            letterSpacing = (-0.72).sp,
            color = tokens.text,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$count morceaux",
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = tokens.textMid,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

@Composable
private fun DocRowWithMenu(
    document: PdfDocumentEntity,
    onClick: () -> Unit,
    onAddToFolder: () -> Unit,
    onDelete: () -> Unit,
) {
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
                text = { Text("Ranger dans un dossier", fontFamily = Inter) },
                onClick = { showMenu = false; onAddToFolder() },
            )
            DropdownMenuItem(
                text = { Text("Supprimer", fontFamily = Inter) },
                onClick = { showMenu = false; onDelete() },
            )
        }
    }
}

@Composable
private fun EmptyCatalogState() {
    val tokens = LocalStudioTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "📥", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Aucun fichier",
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = tokens.textMid,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Importez votre premier PDF avec le bouton +",
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = tokens.textDim,
        )
    }
}
