package com.mazzika.lyrics.ui.home

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.ui.components.DocRow
import com.mazzika.lyrics.ui.components.FeatureCard
import com.mazzika.lyrics.ui.components.QuickAction
import com.mazzika.lyrics.ui.components.QuickTile
import com.mazzika.lyrics.ui.components.SectionHeader
import com.mazzika.lyrics.ui.components.paletteFor
import com.mazzika.lyrics.ui.sync.SyncRole
import com.mazzika.lyrics.ui.sync.SyncViewModel
import com.mazzika.lyrics.ui.theme.CoverGreenA
import com.mazzika.lyrics.ui.theme.CoverGreenB
import com.mazzika.lyrics.ui.theme.CoverOrangeA
import com.mazzika.lyrics.ui.theme.CoverOrangeB
import com.mazzika.lyrics.ui.theme.CoverPinkA
import com.mazzika.lyrics.ui.theme.CoverPinkB
import com.mazzika.lyrics.ui.theme.CoverPurpleA
import com.mazzika.lyrics.ui.theme.CoverPurpleB
import com.mazzika.lyrics.ui.theme.Inter
import com.mazzika.lyrics.ui.theme.LocalStudioTokens
import java.util.Calendar
import kotlinx.coroutines.launch

private val folderIconOptions = listOf("📁", "🎶", "🎸", "⭐", "🎼", "✨", "📦", "🎤")

@Composable
fun HomeScreen(
    onNavigateToReader: (Long) -> Unit,
    onNavigateToSync: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val tokens = LocalStudioTokens.current
    val recentDocuments by viewModel.recentDocuments.collectAsState()

    // Activity-scoped Sync VM to show FeatureCard when a session is active
    val activity = LocalContext.current as ComponentActivity
    val syncViewModel: SyncViewModel = viewModel(activity)
    val syncRole by syncViewModel.role.collectAsState()
    val selectedDocument by syncViewModel.selectedDocument.collectAsState()
    val connectedEndpoints by syncViewModel.connectedEndpoints.collectAsState()
    val isSessionActive = syncRole != SyncRole.NONE

    var showImportDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(tokens.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // ── Greeting
            item { Greeting() }

            // ── Quick tiles 2×2
            item {
                QuickTilesGrid(
                    onCreateSession = onNavigateToSync,
                    onJoinSession = onNavigateToSync,
                    onImport = { showImportDialog = true },
                    onFolders = { /* handled by bottom nav */ },
                )
            }

            // ── Feature card if a session is active
            if (isSessionActive) {
                item {
                    val sessionTitle = selectedDocument?.title ?: "Session en cours"
                    val roleLabel = if (syncRole == SyncRole.PILOT) "Session pilote" else "Session follower"
                    val eyebrow = "● En direct • ${connectedEndpoints.size} connecté${if (connectedEndpoints.size > 1) "s" else ""}"
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                        FeatureCard(
                            eyebrow = eyebrow,
                            title = sessionTitle,
                            meta = roleLabel,
                            onClick = onNavigateToSync,
                        )
                    }
                }
            }

            // ── Section header
            item {
                Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 14.dp)) {
                    SectionHeader(
                        title = "Récemment joué",
                        count = recentDocuments.size.takeIf { it > 0 },
                        link = if (recentDocuments.size > 3) "Voir tout" else null,
                        onLinkClick = {},
                    )
                }
            }

            if (recentDocuments.isEmpty()) {
                item { EmptyRecentState() }
            } else {
                items(recentDocuments) { doc ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        DocRow(
                            title = doc.title,
                            letter = doc.title.firstOrNull()?.uppercase() ?: "?",
                            palette = paletteFor(doc.id),
                            meta = "${doc.pageCount} pages",
                            isPlaying = false,
                            onClick = { onNavigateToReader(doc.id) },
                            onMoreClick = { /* TODO menu */ },
                        )
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        ImportDialog(
            viewModel = viewModel,
            onDismiss = { showImportDialog = false },
        )
    }
}

@Composable
private fun Greeting() {
    val tokens = LocalStudioTokens.current
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 6 -> "Bonne nuit,"
        hour < 18 -> "Bonjour,"
        else -> "Bonsoir,"
    }

    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)) {
        Text(
            text = greeting,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = tokens.textMid,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Musicien",
            fontFamily = Inter,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            letterSpacing = (-0.84).sp,
            lineHeight = 30.sp,
            color = tokens.text,
        )
    }
}

@Composable
private fun QuickTilesGrid(
    onCreateSession: () -> Unit,
    onJoinSession: () -> Unit,
    onImport: () -> Unit,
    onFolders: () -> Unit,
) {
    val actions = listOf(
        QuickAction(
            emoji = "📡",
            labelLine1 = "Créer",
            labelLine2 = "session",
            gradient = Brush.linearGradient(listOf(CoverPurpleA, CoverPurpleB)),
        ) to onCreateSession,
        QuickAction(
            emoji = "🔍",
            labelLine1 = "Rejoindre",
            labelLine2 = "session",
            gradient = Brush.linearGradient(listOf(CoverGreenA, CoverGreenB)),
        ) to onJoinSession,
        QuickAction(
            emoji = "📥",
            labelLine1 = "Importer",
            labelLine2 = "PDF",
            gradient = Brush.linearGradient(listOf(CoverOrangeA, CoverOrangeB)),
        ) to onImport,
        QuickAction(
            emoji = "📁",
            labelLine1 = "Mes",
            labelLine2 = "dossiers",
            gradient = Brush.linearGradient(listOf(CoverPinkA, CoverPinkB)),
        ) to onFolders,
    )

    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile(
                action = actions[0].first,
                onClick = actions[0].second,
                modifier = Modifier.weight(1f),
            )
            QuickTile(
                action = actions[1].first,
                onClick = actions[1].second,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile(
                action = actions[2].first,
                onClick = actions[2].second,
                modifier = Modifier.weight(1f),
            )
            QuickTile(
                action = actions[3].first,
                onClick = actions[3].second,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EmptyRecentState() {
    val tokens = LocalStudioTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🎼", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Aucun fichier récent",
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = tokens.textMid,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Les partitions que vous ouvrez apparaîtront ici.",
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = tokens.textDim,
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// IMPORT DIALOGS
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ImportDialog(
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
) {
    val tokens = LocalStudioTokens.current
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
                pendingUri?.let { uri -> viewModel.importToFolder(uri, folderId) }
                showFolderSelector = false
                pendingUri = null
                onDismiss()
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = tokens.surface,
            title = {
                Text(
                    "Importer un fichier",
                    fontFamily = Inter,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = tokens.text,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ImportChoiceCard(
                        title = "Catalogue uniquement",
                        subtitle = "Ajouter au catalogue sans ranger dans un dossier",
                        onClick = { catalogPickerLauncher.launch(arrayOf("application/pdf")) },
                    )
                    ImportChoiceCard(
                        title = "Dans un dossier",
                        subtitle = "Importer et ranger dans un dossier de votre choix",
                        onClick = { folderPickerLauncher.launch(arrayOf("application/pdf")) },
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Annuler", color = tokens.textMid, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

@Composable
private fun ImportChoiceCard(title: String, subtitle: String, onClick: () -> Unit) {
    val tokens = LocalStudioTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (tokens.isDark) tokens.surfaceHi else tokens.bg)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = tokens.text,
                fontFamily = Inter,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = tokens.textMid,
                fontFamily = Inter,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = tokens.textDim,
        )
    }
}

@Composable
fun FolderSelectorDialog(
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onFolderSelected: (Long) -> Unit,
) {
    val allFolders by viewModel.allFolders.collectAsState()
    FolderSelectorDialogContent(
        allFolders = allFolders,
        hasSubFolders = { folderId -> viewModel.hasSubFolders(folderId) },
        onDismiss = onDismiss,
        onFolderSelected = onFolderSelected,
    )
}

@Composable
fun FolderSelectorDialogContent(
    allFolders: List<FolderEntity>,
    hasSubFolders: suspend (Long) -> Boolean,
    onDismiss: () -> Unit,
    onFolderSelected: (Long) -> Unit,
) {
    val tokens = LocalStudioTokens.current
    val coroutineScope = rememberCoroutineScope()
    val breadcrumb = remember { mutableStateListOf<Pair<Long?, String>>() }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        if (breadcrumb.isEmpty()) breadcrumb.add(null to "Racine")
    }
    val currentParentId = breadcrumb.lastOrNull()?.first
    val currentFolders = allFolders.filter {
        if (currentParentId == null) it.parentFolderId == null else it.parentFolderId == currentParentId
    }
    LaunchedEffect(currentParentId) {
        if (currentParentId != null) selectedFolderId = currentParentId
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.surface,
        title = {
            Column {
                Text(
                    "Choisir un dossier",
                    fontFamily = Inter,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = tokens.text,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    breadcrumb.forEachIndexed { index, (folderId, name) ->
                        if (index > 0) Text(" > ", color = tokens.textDim, fontSize = 12.sp)
                        Text(
                            text = name,
                            fontFamily = Inter,
                            color = if (index == breadcrumb.lastIndex) tokens.gold else tokens.textMid,
                            fontSize = 12.sp,
                            fontWeight = if (index == breadcrumb.lastIndex) FontWeight.SemiBold else FontWeight.Medium,
                            modifier = Modifier.clickable {
                                while (breadcrumb.size > index + 1) breadcrumb.removeAt(breadcrumb.lastIndex)
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
                    "Aucun sous-dossier",
                    color = tokens.textMid,
                    fontFamily = Inter,
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
                            modifier = Modifier.clickable {
                                coroutineScope.launch {
                                    val hasSubs = hasSubFolders(folder.id)
                                    if (hasSubs) {
                                        breadcrumb.add(folder.id to folder.name)
                                        selectedFolderId = folder.id
                                    } else {
                                        selectedFolderId = folder.id
                                    }
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) tokens.gold.copy(alpha = 0.18f)
                                else if (tokens.isDark) tokens.surfaceHi else tokens.bg,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = if (isSelected) BorderStroke(1.dp, tokens.gold) else null,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(text = folder.icon ?: "📁", fontSize = 24.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = folder.name,
                                    color = tokens.text,
                                    fontFamily = Inter,
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
                onClick = { selectedFolderId?.let { onFolderSelected(it) } },
                enabled = selectedFolderId != null,
            ) {
                val selectedName = allFolders.find { it.id == selectedFolderId }?.name ?: ""
                Text(
                    text = if (selectedName.isNotEmpty()) "Choisir \"$selectedName\"" else "Choisir",
                    color = if (selectedFolderId != null) tokens.gold else tokens.textDim,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                )
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
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, icon: String?) -> Unit,
) {
    val tokens = LocalStudioTokens.current
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<String?>("📁") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.surface,
        title = {
            Text(
                "Nouveau dossier",
                fontFamily = Inter,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = tokens.text,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du dossier", fontFamily = Inter) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Icône",
                    color = tokens.textMid,
                    fontFamily = Inter,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                val chunked = folderIconOptions.chunked(4)
                chunked.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { icon ->
                            val isSelected = selectedIcon == icon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) tokens.gold.copy(alpha = 0.18f)
                                        else if (tokens.isDark) tokens.surfaceHi else tokens.bg,
                                    )
                                    .clickable { selectedIcon = icon },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = icon, fontSize = 22.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim(), selectedIcon) },
            ) {
                Text("Créer", color = tokens.gold, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = tokens.textMid, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}
