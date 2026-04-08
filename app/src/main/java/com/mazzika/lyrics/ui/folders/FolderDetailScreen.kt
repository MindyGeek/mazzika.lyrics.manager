package com.mazzika.lyrics.ui.folders

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = folder?.icon ?: "📁",
                            fontSize = 20.sp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = folder?.name ?: "",
                            color = DarkTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = DarkTextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = DarkTextPrimary,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateFolderDialog = true },
                containerColor = Gold,
                contentColor = DarkBackground,
                shape = CircleShape,
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Créer un sous-dossier")
            }
        },
    ) { innerPadding ->
        if (subFolders.isEmpty() && documents.isEmpty()) {
            EmptyFolderState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onCreateSubFolder = { showCreateFolderDialog = true },
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                // Sub-folders section
                if (subFolders.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Sous-dossiers")
                    }
                    item {
                        SubFolderRow(
                            folders = subFolders,
                            onFolderClick = { onNavigateToFolder(it.id) },
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Documents section
                if (documents.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Fichiers")
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
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        color = DarkTextPrimary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
    )
}

@Composable
private fun SubFolderRow(
    folders: List<FolderEntity>,
    onFolderClick: (FolderEntity) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(folders) { folder ->
            SubFolderCard(
                folder = folder,
                onClick = { onFolderClick(folder) },
            )
        }
    }
}

@Composable
private fun SubFolderCard(
    folder: FolderEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = folder.icon ?: "📁",
                fontSize = 32.sp,
            )
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
}

@Composable
private fun FolderDocumentItem(
    document: PdfDocumentEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

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
                fontSize = 12.sp,
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
            text = "Appuyez sur + pour créer un sous-dossier",
            color = DarkTextMuted,
            fontSize = 13.sp,
        )
    }
}
