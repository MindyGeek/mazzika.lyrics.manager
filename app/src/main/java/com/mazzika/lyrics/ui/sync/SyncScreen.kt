package com.mazzika.lyrics.ui.sync

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.data.nearby.NearbySessionManager
import com.mazzika.lyrics.ui.theme.DarkBackground
import com.mazzika.lyrics.ui.theme.DarkSurface
import com.mazzika.lyrics.ui.theme.DarkSurfaceElevated
import com.mazzika.lyrics.ui.theme.DarkTextMuted
import com.mazzika.lyrics.ui.theme.DarkTextPrimary
import com.mazzika.lyrics.ui.theme.DarkTextSecondary
import com.mazzika.lyrics.ui.theme.Error
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.GoldDeep
import com.mazzika.lyrics.ui.theme.Success

@Composable
fun SyncScreen(
    onNavigateToReaderSync: (String) -> Unit,
    viewModel: SyncViewModel = viewModel(),
) {
    val role by viewModel.role.collectAsState()
    val selectedDocument by viewModel.selectedDocument.collectAsState()
    val syncFilePath by viewModel.syncFilePath.collectAsState()
    val allDocuments by viewModel.allDocuments.collectAsState()
    val connectedEndpoints by viewModel.connectedEndpoints.collectAsState()
    val discoveredEndpoints by viewModel.discoveredEndpoints.collectAsState()

    // Navigate to reader when a file is ready
    LaunchedEffect(syncFilePath) {
        syncFilePath?.let { path ->
            onNavigateToReaderSync(path)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
    ) { innerPadding ->
        when (role) {
            SyncRole.NONE -> NoneState(
                allDocuments = allDocuments,
                innerPadding = innerPadding,
                onCreateSession = { doc -> viewModel.startAsPilot(doc) },
                onJoinSession = { viewModel.startAsFollower() },
            )
            SyncRole.PILOT -> PilotState(
                selectedDocument = selectedDocument,
                connectedEndpoints = connectedEndpoints,
                innerPadding = innerPadding,
                onAnnounce = { viewModel.announcePilotSession() },
                onStop = { viewModel.stopSession() },
            )
            SyncRole.FOLLOWER -> FollowerState(
                discoveredEndpoints = discoveredEndpoints,
                innerPadding = innerPadding,
                onJoin = { endpointId -> viewModel.connectToEndpoint(endpointId) },
                onCancel = { viewModel.stopSession() },
            )
        }
    }
}

@Composable
private fun NoneState(
    allDocuments: List<PdfDocumentEntity>,
    innerPadding: PaddingValues,
    onCreateSession: (PdfDocumentEntity) -> Unit,
    onJoinSession: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        item {
            Text(
                text = "Synchronisation",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                color = DarkTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // Mode cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Create session card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Wifi,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Créer session",
                            color = DarkTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Partager une partition",
                            color = DarkTextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // Join session card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onJoinSession() },
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Rejoindre",
                            color = DarkTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Rejoindre une session",
                            color = DarkTextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Document picker
        item {
            Text(
                text = "Choisir une partition à partager",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = DarkTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (allDocuments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Aucune partition dans le catalogue",
                        color = DarkTextMuted,
                        fontSize = 14.sp,
                    )
                }
            }
        } else {
            items(allDocuments) { doc ->
                DocumentPickerItem(
                    document = doc,
                    onClick = { onCreateSession(doc) },
                )
            }
        }
    }
}

@Composable
private fun DocumentPickerItem(
    document: PdfDocumentEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
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
                    fontSize = 12.sp,
                )
            }
            Text(
                text = "Partager",
                color = Gold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PilotState(
    selectedDocument: PdfDocumentEntity?,
    connectedEndpoints: List<NearbySessionManager.EndpointInfo>,
    innerPadding: PaddingValues,
    onAnnounce: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(20.dp),
    ) {
        Text(
            text = "Session active",
            color = DarkTextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Active session card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Success)
                        }
                    }
                    Text(
                        text = "Diffusion en cours",
                        color = Success,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedDocument != null) {
                    Text(
                        text = selectedDocument.title,
                        color = DarkTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${selectedDocument.pageCount} pages",
                        color = DarkTextMuted,
                        fontSize = 13.sp,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.People,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "${connectedEndpoints.size} appareil(s) connecté(s)",
                        color = DarkTextSecondary,
                        fontSize = 14.sp,
                    )
                }

                if (connectedEndpoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    connectedEndpoints.forEach { endpoint ->
                        Text(
                            text = "• ${endpoint.name}",
                            color = DarkTextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Announce button
        Button(
            onClick = onAnnounce,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldDeep,
                contentColor = DarkTextPrimary,
            ),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("Annoncer la partition", fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stop button
        OutlinedButton(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
            border = BorderStroke(1.dp, Error),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp),
            )
            Text("Arrêter la session", fontSize = 15.sp)
        }
    }
}

@Composable
private fun FollowerState(
    discoveredEndpoints: List<NearbySessionManager.EndpointInfo>,
    innerPadding: PaddingValues,
    onJoin: (String) -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(20.dp),
    ) {
        Text(
            text = "Rejoindre",
            color = DarkTextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Searching indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(
                    color = Gold,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
                Column {
                    Text(
                        text = "Recherche de sessions...",
                        color = DarkTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "${discoveredEndpoints.size} appareil(s) trouvé(s)",
                        color = DarkTextMuted,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Discovered endpoints list
        if (discoveredEndpoints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Aucune session trouvée",
                        color = DarkTextMuted,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = "Assurez-vous que le chef de pupitre diffuse une session",
                        color = DarkTextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
                    )
                }
            }
        } else {
            val isTablet = LocalConfiguration.current.screenWidthDp >= 600
            if (isTablet) {
                val rows = discoveredEndpoints.chunked(2)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rows.forEach { rowEndpoints ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowEndpoints.forEach { endpoint ->
                                Box(modifier = Modifier.weight(1f)) {
                                    DiscoveredEndpointItem(
                                        endpoint = endpoint,
                                        onJoin = { onJoin(endpoint.id) },
                                    )
                                }
                            }
                            if (rowEndpoints.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    discoveredEndpoints.forEach { endpoint ->
                        DiscoveredEndpointItem(
                            endpoint = endpoint,
                            onJoin = { onJoin(endpoint.id) },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cancel button
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Annuler",
                color = DarkTextSecondary,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun DiscoveredEndpointItem(
    endpoint: NearbySessionManager.EndpointInfo,
    onJoin: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color = GoldDeep, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        text = endpoint.name,
                        color = DarkTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = endpoint.id,
                        color = DarkTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Button(
                onClick = onJoin,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = DarkBackground,
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Rejoindre",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
