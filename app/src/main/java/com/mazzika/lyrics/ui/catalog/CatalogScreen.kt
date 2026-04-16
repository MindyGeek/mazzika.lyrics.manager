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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.ui.sync.CreateSessionDialog
import com.mazzika.lyrics.ui.sync.SyncViewModel
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
}

@Composable
fun CatalogScreen(
    onNavigateToReader: (Long) -> Unit,
    onNavigateToSync: () -> Unit = {},
    viewModel: CatalogViewModel = viewModel(),
) {
    val tokens = LocalStudioTokens.current
    val documents by viewModel.documents.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val allFolders by viewModel.allFolders.collectAsState()

    // Create-session dialog — Activity-scoped SyncViewModel for session management.
    val activity = LocalContext.current as ComponentActivity
    val syncViewModel: SyncViewModel = viewModel(activity)
    val allDocsForSession by syncViewModel.allDocuments.collectAsState()
    val sessionName by syncViewModel.sessionName.collectAsState()
    var showCreateSession by remember { mutableStateOf(false) }
    var preSelectedDoc by remember { mutableStateOf<PdfDocumentEntity?>(null) }

    var documentToAddToFolder by remember { mutableStateOf<Long?>(null) }
    var documentToDelete by remember { mutableStateOf<PdfDocumentEntity?>(null) }
    var selectedChip by remember { mutableStateOf(CatalogChip.RECENT) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.importPdf(it) } }

    Box(modifier = Modifier.fillMaxSize().background(tokens.bg)) {
        // Sticky header (Column that stays fixed) + scrolling list below
        Column(modifier = Modifier.fillMaxSize()) {
            CatalogStickyHeader(
                count = documents.size,
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                selectedChip = selectedChip,
                onChipSelected = { chip ->
                    selectedChip = chip
                    when (chip) {
                        CatalogChip.RECENT -> viewModel.setSortMode(SortMode.RECENT)
                        CatalogChip.A_Z -> viewModel.setSortMode(SortMode.TITLE)
                    }
                },
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
            ) {
                if (documents.isEmpty() && !isImporting) {
                    item { EmptyCatalogState() }
                } else {
                    items(documents) { doc ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            DocRowWithMenu(
                                document = doc,
                                onClick = { onNavigateToReader(doc.id) },
                                onAddToFolder = { documentToAddToFolder = doc.id },
                                onDelete = { documentToDelete = doc },
                                onShareSession = {
                                    syncViewModel.initSessionName()
                                    preSelectedDoc = doc
                                    showCreateSession = true
                                },
                            )
                        }
                    }
                }
            }
        }

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

        StudioFab(
            onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Importer", modifier = Modifier.size(28.dp))
        }
    }

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

    val docToDelete = documentToDelete
    if (docToDelete != null) {
        DeleteConfirmDialog(
            title = docToDelete.title,
            onDismiss = { documentToDelete = null },
            onConfirm = {
                viewModel.deleteDocument(docToDelete.id)
                documentToDelete = null
            },
        )
    }

    if (showCreateSession) {
        CreateSessionDialog(
            sessionName = sessionName,
            onSessionNameChange = { syncViewModel.setSessionName(it) },
            allDocuments = allDocsForSession,
            preSelectedDocument = preSelectedDoc,
            onDismiss = {
                showCreateSession = false
                preSelectedDoc = null
            },
            onStart = { doc ->
                syncViewModel.startAsPilot(doc)
                showCreateSession = false
                preSelectedDoc = null
                onNavigateToSync()
            },
        )
    }
}

@Composable
private fun CatalogStickyHeader(
    count: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedChip: CatalogChip,
    onChipSelected: (CatalogChip) -> Unit,
) {
    val tokens = LocalStudioTokens.current
    Column(modifier = Modifier.background(tokens.bg)) {
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
        Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
            SearchInputBar(value = query, onValueChange = onQueryChange)
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(CatalogChip.entries) { chip ->
                FilterChip(
                    label = chip.label,
                    selected = chip == selectedChip,
                    onClick = { onChipSelected(chip) },
                )
            }
        }
    }
}

@Composable
private fun DocRowWithMenu(
    document: PdfDocumentEntity,
    onClick: () -> Unit,
    onAddToFolder: () -> Unit,
    onDelete: () -> Unit,
    onShareSession: () -> Unit,
) {
    val tokens = LocalStudioTokens.current
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        DocRow(
            title = document.title,
            letter = document.title.firstOrNull()?.uppercase() ?: "?",
            palette = paletteFor(document.id),
            meta = "${document.pageCount} pages",
            onClick = onClick,
            onMoreClick = { showMenu = true },
        )
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Ouvrir", fontFamily = Inter, color = tokens.text) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp), tint = tokens.textMid) },
                    onClick = { showMenu = false; onClick() },
                )
                DropdownMenuItem(
                    text = { Text("Partager en session", fontFamily = Inter, color = tokens.text) },
                    leadingIcon = { Icon(Icons.Filled.CellTower, null, Modifier.size(18.dp), tint = tokens.textMid) },
                    onClick = { showMenu = false; onShareSession() },
                )
                DropdownMenuItem(
                    text = { Text("Ranger dans un dossier", fontFamily = Inter, color = tokens.text) },
                    leadingIcon = { Icon(Icons.Filled.Folder, null, Modifier.size(18.dp), tint = tokens.textMid) },
                    onClick = { showMenu = false; onAddToFolder() },
                )
                DropdownMenuItem(
                    text = { Text("Supprimer", fontFamily = Inter, color = tokens.danger) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, Modifier.size(18.dp), tint = tokens.danger) },
                    onClick = { showMenu = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val tokens = LocalStudioTokens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                "Supprimer ce fichier ?",
                color = tokens.text,
                fontFamily = Inter,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
            )
        },
        text = {
            Text(
                text = "« $title » sera retiré du catalogue. Cette action est irréversible.",
                color = tokens.textMid,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Supprimer", color = tokens.danger, fontFamily = Inter, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = tokens.textMid, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
            }
        },
    )
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
