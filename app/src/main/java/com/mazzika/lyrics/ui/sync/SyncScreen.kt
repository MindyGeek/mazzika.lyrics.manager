package com.mazzika.lyrics.ui.sync

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.data.nearby.NearbySessionManager
import com.mazzika.lyrics.ui.components.ActiveSessionCard
import com.mazzika.lyrics.ui.components.DangerOutlineButton
import com.mazzika.lyrics.ui.components.FoundSessionCard
import com.mazzika.lyrics.ui.components.GroupLabel
import com.mazzika.lyrics.ui.components.LiveBanner
import com.mazzika.lyrics.ui.components.PrimaryGoldButton
import com.mazzika.lyrics.ui.components.PulseSearch
import com.mazzika.lyrics.ui.components.SessionChoiceCard
import com.mazzika.lyrics.ui.components.paletteFor
import com.mazzika.lyrics.ui.theme.CoverGreenA
import com.mazzika.lyrics.ui.theme.CoverGreenB
import com.mazzika.lyrics.ui.theme.CoverPurpleA
import com.mazzika.lyrics.ui.theme.CoverPurpleB
import com.mazzika.lyrics.ui.theme.Inter
import com.mazzika.lyrics.ui.theme.LocalStudioTokens

@Composable
fun SyncScreen(
    onOpenSharedFile: () -> Unit,
    onNavigateToReader: (Long) -> Unit,
    viewModel: SyncViewModel = viewModel(),
) {
    val tokens = LocalStudioTokens.current
    val context = LocalContext.current
    val role by viewModel.role.collectAsState()
    val selectedDocument by viewModel.selectedDocument.collectAsState()
    val syncFilePath by viewModel.syncFilePath.collectAsState()
    val allDocuments by viewModel.allDocuments.collectAsState()
    val connectedEndpoints by viewModel.connectedEndpoints.collectAsState()
    val discoveredEndpoints by viewModel.discoveredEndpoints.collectAsState()
    val sessionName by viewModel.sessionName.collectAsState()
    val sessionEndedByPilot by viewModel.sessionEndedByPilot.collectAsState()
    val discoveryTimedOut by viewModel.discoveryTimedOut.collectAsState()
    val isTempFile by viewModel.isTempFile.collectAsState()

    // Permissions
    val requiredPermissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) pendingAction?.invoke()
        pendingAction = null
    }
    fun requestPermissionsAndRun(action: () -> Unit) {
        val granted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) action() else {
            pendingAction = action
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    // Session ended by pilot
    if (sessionEndedByPilot) {
        SessionEndedDialog(
            isTempFile = isTempFile,
            onSave = { viewModel.saveToCatalogue() },
            onDismiss = {
                viewModel.acknowledgeSessionEnd()
                viewModel.stopSession()
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(tokens.bg)) {
        when (role) {
            SyncRole.PILOT -> {
                PilotState(
                    selectedDocument = selectedDocument,
                    sessionName = sessionName,
                    connectedEndpoints = connectedEndpoints,
                    onOpenReader = {
                        selectedDocument?.let { doc -> onNavigateToReader(doc.id) }
                    },
                    onStopSession = { viewModel.stopSession() },
                )
            }
            SyncRole.FOLLOWER -> {
                when {
                    // File received → show the session details landing.
                    syncFilePath != null -> {
                        FollowerSessionDetails(
                            fileTitle = selectedDocument?.title
                                ?: java.io.File(syncFilePath ?: "").nameWithoutExtension,
                            pageCount = selectedDocument?.pageCount,
                            pilotName = connectedEndpoints.firstOrNull()?.name ?: "Pilote",
                            connectedCount = connectedEndpoints.size + 1, // +1 for self
                            isTempFile = isTempFile,
                            onOpenFile = onOpenSharedFile,
                            onLeave = { showLeaveConfirm = true },
                            onSave = { viewModel.saveToCatalogue() },
                        )
                    }
                    // Linked to a pilot but file not yet received → overlay on pulse search
                    connectedEndpoints.isNotEmpty() -> {
                        JoiningState(
                            discoveryTimedOut = discoveryTimedOut,
                            discoveredEndpoints = discoveredEndpoints,
                            isConnecting = true,
                            connectingToName = connectedEndpoints.firstOrNull()?.name,
                            onBack = { viewModel.stopSession() },
                            onRetry = { viewModel.restartDiscovery() },
                            onJoin = { endpoint -> viewModel.connectToEndpoint(endpoint.id) },
                        )
                    }
                    // Still discovering
                    else -> {
                        JoiningState(
                            discoveryTimedOut = discoveryTimedOut,
                            discoveredEndpoints = discoveredEndpoints,
                            isConnecting = false,
                            connectingToName = null,
                            onBack = { viewModel.stopSession() },
                            onRetry = { viewModel.restartDiscovery() },
                            onJoin = { endpoint -> viewModel.connectToEndpoint(endpoint.id) },
                        )
                    }
                }
            }
            SyncRole.NONE -> {
                NoneState(
                    onCreateSession = {
                        requestPermissionsAndRun {
                            viewModel.initSessionName()
                            showCreateDialog = true
                        }
                    },
                    onJoinSession = {
                        requestPermissionsAndRun {
                            viewModel.startAsFollower()
                        }
                    },
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateSessionDialog(
            sessionName = sessionName,
            onSessionNameChange = { viewModel.setSessionName(it) },
            allDocuments = allDocuments,
            onDismiss = { showCreateDialog = false },
            onStart = { doc ->
                viewModel.startAsPilot(doc)
                showCreateDialog = false
            },
        )
    }

    if (showLeaveConfirm) {
        LeaveSessionConfirmDialog(
            onDismiss = { showLeaveConfirm = false },
            onConfirm = {
                showLeaveConfirm = false
                viewModel.stopSession()
            },
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// NONE STATE
// ═════════════════════════════════════════════════════════════════

@Composable
private fun NoneState(
    onCreateSession: () -> Unit,
    onJoinSession: () -> Unit,
) {
    val tokens = LocalStudioTokens.current
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(30.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "NEARBY CONNECTIONS",
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.65.sp,
                color = tokens.gold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Synchronisation\nen direct",
                fontFamily = Inter,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                letterSpacing = (-0.78).sp,
                lineHeight = 28.sp,
                color = tokens.text,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Partagez la même partition avec votre équipe. Tournez les pages ensemble, sans internet.",
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = tokens.textMid,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        Spacer(Modifier.height(32.dp))
        SessionChoiceCard(
            emoji = "📡",
            title = "Créer une session",
            description = "Devenez pilote et diffusez à votre équipe",
            iconGradient = Brush.linearGradient(listOf(CoverPurpleA, CoverPurpleB)),
            onClick = onCreateSession,
        )
        Spacer(Modifier.height(12.dp))
        SessionChoiceCard(
            emoji = "🔍",
            title = "Rejoindre une session",
            description = "Recherchez les sessions actives à proximité",
            iconGradient = Brush.linearGradient(listOf(CoverGreenA, CoverGreenB)),
            onClick = onJoinSession,
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// JOINING STATE — pulse search + found sessions list
// ═════════════════════════════════════════════════════════════════

@Composable
private fun JoiningState(
    discoveryTimedOut: Boolean,
    discoveredEndpoints: List<NearbySessionManager.EndpointInfo>,
    isConnecting: Boolean,
    connectingToName: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onJoin: (NearbySessionManager.EndpointInfo) -> Unit,
) {
    val tokens = LocalStudioTokens.current
    val hasFound = discoveredEndpoints.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Hero zone: animated pulse while searching, static "no-signal" glyph when
            // discovery timed out with no results.
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val emptyAfterTimeout = discoveryTimedOut && !hasFound

                if (emptyAfterTimeout) {
                    NoSignalGlyph()
                } else {
                    PulseSearch()
                }

                Spacer(Modifier.height(16.dp))

                val (title, subtitle) = when {
                    hasFound && !discoveryTimedOut ->
                        "À la recherche..." to "${discoveredEndpoints.size} session${if (discoveredEndpoints.size > 1) "s" else ""} trouvée${if (discoveredEndpoints.size > 1) "s" else ""}"
                    hasFound && discoveryTimedOut ->
                        "${discoveredEndpoints.size} session${if (discoveredEndpoints.size > 1) "s" else ""} trouvée${if (discoveredEndpoints.size > 1) "s" else ""}" to "Appuyez sur « Rejoindre » pour continuer"
                    discoveryTimedOut ->
                        "Aucune session trouvée" to "Vérifiez que le pilote a bien démarré la session."
                    else ->
                        "À la recherche..." to "Assurez-vous que le Bluetooth et le Wi-Fi sont activés"
                }

                Text(
                    text = title,
                    fontFamily = Inter,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.4).sp,
                    color = tokens.text,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = tokens.textMid,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 28.dp),
                )
                if (discoveryTimedOut) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(tokens.surfaceHi)
                            .border(1.dp, tokens.cardBorder, RoundedCornerShape(20.dp))
                            .clickable { onRetry() }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = tokens.gold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Réessayer",
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = tokens.text,
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(discoveredEndpoints) { endpoint ->
                    FoundSessionCard(
                        title = endpoint.name,
                        host = "Appareil à proximité",
                        coverLetter = endpoint.name.firstOrNull()?.uppercase() ?: "?",
                        coverGradient = Brush.linearGradient(
                            listOf(paletteFor(endpoint.id).a, paletteFor(endpoint.id).b),
                        ),
                        onJoin = { onJoin(endpoint) },
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                DangerOutlineButton(
                    text = "Quitter la recherche",
                    onClick = onBack,
                    leadingContent = {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                )
            }
        }

        // "Connexion en cours..." fully-opaque overlay while joining a specific session
        if (isConnecting) {
            ConnectingOverlay(
                sessionName = connectingToName,
                onCancel = onBack,
            )
        }
    }
}

/** Static glyph shown when discovery times out and no sessions were found. */
@Composable
private fun NoSignalGlyph() {
    val tokens = LocalStudioTokens.current
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer muted ring
        Box(
            modifier = Modifier
                .size(120.dp)
                .border(2.dp, tokens.textDim.copy(alpha = 0.25f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(2.dp, tokens.textDim.copy(alpha = 0.35f), CircleShape),
        )
        // Inner muted disk with a ban symbol in the gold color
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(tokens.surfaceHi),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "🛰",
                fontSize = 24.sp,
            )
        }
        // Diagonal strike-through spanning the whole glyph
        androidx.compose.foundation.Canvas(modifier = Modifier.size(120.dp)) {
            val stroke = 3.dp.toPx()
            drawLine(
                color = tokens.danger.copy(alpha = 0.85f),
                start = androidx.compose.ui.geometry.Offset(size.width * 0.22f, size.height * 0.22f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.78f),
                strokeWidth = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ConnectingOverlay(
    sessionName: String?,
    onCancel: () -> Unit,
) {
    val tokens = LocalStudioTokens.current
    // Fully opaque so the discovery list never peeks through underneath.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.bg)
            .clickable(enabled = false, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = tokens.gold, strokeWidth = 3.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                text = if (sessionName.isNullOrBlank()) "Connexion en cours..." else "Connexion à « $sessionName »...",
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = tokens.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Réception du fichier en cours.",
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = tokens.textMid,
            )
            Spacer(Modifier.height(24.dp))
            DangerOutlineButton(
                text = "Quitter la session",
                onClick = onCancel,
                modifier = Modifier.padding(horizontal = 60.dp),
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// FOLLOWER SESSION DETAILS — landing after joining a session
// ═════════════════════════════════════════════════════════════════

@Composable
private fun FollowerSessionDetails(
    fileTitle: String,
    pageCount: Int?,
    pilotName: String,
    connectedCount: Int,
    isTempFile: Boolean,
    onOpenFile: () -> Unit,
    onLeave: () -> Unit,
    onSave: () -> Unit,
) {
    val tokens = LocalStudioTokens.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Live banner
        LiveBanner(label = "Session en cours", timer = "")

        Spacer(Modifier.height(16.dp))

        // File card
        val palette = paletteFor(fileTitle)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(tokens.cardElevation, RoundedCornerShape(16.dp), clip = false)
                .clip(RoundedCornerShape(16.dp))
                .background(if (tokens.isDark) tokens.surfaceHi else tokens.surface)
                .then(
                    if (!tokens.isDark)
                        Modifier.border(1.dp, tokens.cardBorder, RoundedCornerShape(16.dp))
                    else Modifier,
                )
                .padding(20.dp),
        ) {
            // File header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(palette.a, palette.b))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = fileTitle.firstOrNull()?.uppercase() ?: "?",
                        fontFamily = Inter,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileTitle,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = tokens.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (pageCount != null) "$pageCount page${if (pageCount > 1) "s" else ""}" else "Partition partagée",
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = tokens.textMid,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Info rows
            InfoRow(label = "Pilote", value = pilotName)
            Spacer(Modifier.height(10.dp))
            InfoRow(
                label = "Participants",
                value = "$connectedCount connecté${if (connectedCount > 1) "s" else ""}",
            )

            if (isTempFile) {
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(tokens.gold.copy(alpha = 0.1f))
                        .clickable(onClick = onSave)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, tint = tokens.gold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sauvegarder dans le catalogue",
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = tokens.text,
                        )
                        Text(
                            text = "Le fichier est temporaire",
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = tokens.textMid,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Actions
        PrimaryGoldButton(
            text = "Ouvrir la partition",
            onClick = onOpenFile,
            leadingContent = {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            },
        )
        Spacer(Modifier.height(10.dp))
        DangerOutlineButton(
            text = "Quitter la session",
            onClick = onLeave,
            leadingContent = {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val tokens = LocalStudioTokens.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label.uppercase(),
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            color = tokens.textDim,
            modifier = Modifier.width(110.dp),
        )
        Text(
            text = value,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = tokens.text,
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// LEAVE SESSION CONFIRMATION DIALOG
// ═════════════════════════════════════════════════════════════════

@Composable
private fun LeaveSessionConfirmDialog(
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
                "Quitter la session ?",
                color = tokens.text,
                fontFamily = Inter,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
            )
        },
        text = {
            Text(
                text = "Vous serez déconnecté de la session en cours. Vous pourrez rejoindre à nouveau plus tard.",
                color = tokens.textMid,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Quitter", color = tokens.danger, fontFamily = Inter, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = tokens.textMid, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

// ═════════════════════════════════════════════════════════════════
// PILOT STATE — live banner + active session card
// ═════════════════════════════════════════════════════════════════

@Composable
private fun PilotState(
    selectedDocument: PdfDocumentEntity?,
    sessionName: String,
    connectedEndpoints: List<NearbySessionManager.EndpointInfo>,
    onOpenReader: () -> Unit,
    onStopSession: () -> Unit,
) {
    val tokens = LocalStudioTokens.current
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            elapsedSeconds++
        }
    }
    val timerText = remember(elapsedSeconds) {
        val m = elapsedSeconds / 60
        val s = elapsedSeconds % 60
        "%02d:%02d".format(m, s)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        LiveBanner(timer = timerText)
        Spacer(Modifier.height(16.dp))

        val doc = selectedDocument
        if (doc != null) {
            val palette = paletteFor(doc.id)
            ActiveSessionCard(
                fileTitle = doc.title,
                subtitle = "$sessionName • ${doc.pageCount} page${if (doc.pageCount > 1) "s" else ""}",
                coverLetter = doc.title.firstOrNull()?.uppercase() ?: "?",
                coverGradient = Brush.linearGradient(listOf(palette.a, palette.b)),
                devicesCount = connectedEndpoints.size,
                devices = connectedEndpoints.map {
                    Triple(
                        it.name.firstOrNull()?.uppercase() ?: "?",
                        it.name,
                        "Connecté",
                    )
                },
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryGoldButton(
                            text = "Ouvrir le lecteur",
                            onClick = onOpenReader,
                            leadingContent = {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                        )
                        DangerOutlineButton(
                            text = "Arrêter la diffusion",
                            onClick = onStopSession,
                            leadingContent = {
                                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                        )
                    }
                },
            )
        } else {
            Text(
                "Aucun document sélectionné.",
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                color = tokens.textMid,
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// CREATE SESSION DIALOG — 3 steps
// ═════════════════════════════════════════════════════════════════

@Composable
private fun CreateSessionDialog(
    sessionName: String,
    onSessionNameChange: (String) -> Unit,
    allDocuments: List<PdfDocumentEntity>,
    onDismiss: () -> Unit,
    onStart: (PdfDocumentEntity) -> Unit,
) {
    val tokens = LocalStudioTokens.current
    var currentStep by remember { mutableIntStateOf(0) }
    var selectedDoc by remember { mutableStateOf<PdfDocumentEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 24.dp),
        containerColor = tokens.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "Créer une session",
                    color = tokens.text,
                    fontFamily = Inter,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(10.dp))
                StepIndicator(currentStep = currentStep, totalSteps = 3)
            }
        },
        text = {
            when (currentStep) {
                0 -> Column {
                    Text(
                        "Nom de la session",
                        color = tokens.textMid,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sessionName,
                        onValueChange = onSessionNameChange,
                        label = { Text("Ex. Répétition Hadhra", fontFamily = Inter) },
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
                }
                1 -> DocumentPickerStep(
                    documents = allDocuments,
                    selectedDoc = selectedDoc,
                    onSelect = { selectedDoc = it },
                )
                2 -> ConfirmStep(sessionName = sessionName, selectedDoc = selectedDoc)
            }
        },
        // All action buttons live in the single `confirmButton` slot so they never
        // compete for space with Material's split confirm/dismiss layout.
        // Primary gold action is full-width on top; secondary text buttons sit below.
        confirmButton = {
            val canNext = when (currentStep) {
                0 -> sessionName.isNotBlank()
                1 -> selectedDoc != null
                2 -> selectedDoc != null
                else -> false
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                PrimaryGoldButton(
                    text = if (currentStep == 2) "Démarrer la session" else "Suivant",
                    onClick = {
                        if (currentStep == 2) selectedDoc?.let { onStart(it) }
                        else currentStep++
                    },
                    enabled = canNext,
                    leadingContent = if (currentStep == 2) {
                        {
                            Icon(
                                Icons.Filled.CellTower,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else null,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (currentStep > 0) {
                        TextButton(onClick = { currentStep-- }) {
                            Text(
                                "Précédent",
                                color = tokens.textMid,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            "Annuler",
                            color = tokens.textMid,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
        dismissButton = null,
    )
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    val tokens = LocalStudioTokens.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(totalSteps) { i ->
            Box(
                modifier = Modifier
                    .size(width = if (i == currentStep) 22.dp else 6.dp, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (i <= currentStep) Brush.linearGradient(listOf(tokens.goldLight, tokens.gold))
                        else Brush.linearGradient(listOf(tokens.pillBg, tokens.pillBg)),
                    ),
            )
        }
    }
}

@Composable
private fun DocumentPickerStep(
    documents: List<PdfDocumentEntity>,
    selectedDoc: PdfDocumentEntity?,
    onSelect: (PdfDocumentEntity) -> Unit,
) {
    val tokens = LocalStudioTokens.current
    Column {
        Text(
            "Choisissez une partition",
            color = tokens.textMid,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(10.dp))
        if (documents.isEmpty()) {
            Text(
                "Aucun document disponible.\nImportez un PDF depuis le Catalogue.",
                color = tokens.textDim,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(documents) { doc ->
                    val isSelected = selectedDoc?.id == doc.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) tokens.gold.copy(alpha = 0.14f) else tokens.surfaceHi)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) tokens.gold else Color.Transparent,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { onSelect(doc) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val palette = paletteFor(doc.id)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(palette.a, palette.b))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = doc.title.firstOrNull()?.uppercase() ?: "?",
                                fontFamily = Inter,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.title,
                                color = tokens.text,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${doc.pageCount} page${if (doc.pageCount > 1) "s" else ""}",
                                color = tokens.textMid,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmStep(sessionName: String, selectedDoc: PdfDocumentEntity?) {
    val tokens = LocalStudioTokens.current
    Column {
        ConfirmRow("Session", sessionName)
        Spacer(Modifier.height(10.dp))
        ConfirmRow("Partition", selectedDoc?.title ?: "—")
        Spacer(Modifier.height(10.dp))
        ConfirmRow("Pages", selectedDoc?.pageCount?.toString() ?: "—")
        Spacer(Modifier.height(16.dp))
        Text(
            "Votre appareil deviendra pilote de la session. Les autres musiciens pourront se connecter à proximité.",
            color = tokens.textMid,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun ConfirmRow(label: String, value: String) {
    val tokens = LocalStudioTokens.current
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label.uppercase(),
            color = tokens.textDim,
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier.width(90.dp),
        )
        Text(
            text = value,
            color = tokens.text,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// SESSION ENDED DIALOG
// ═════════════════════════════════════════════════════════════════

@Composable
private fun SessionEndedDialog(
    isTempFile: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalStudioTokens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                "Session terminée",
                color = tokens.text,
                fontFamily = Inter,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
            )
        },
        text = {
            Text(
                text = if (isTempFile)
                    "Le pilote a arrêté la session. Voulez-vous conserver le fichier reçu dans votre catalogue ?"
                else
                    "La session a été arrêtée par le pilote.",
                color = tokens.textMid,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        },
        confirmButton = {
            if (isTempFile) {
                TextButton(onClick = { onSave(); onDismiss() }) {
                    Text("Garder", color = tokens.gold, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("OK", color = tokens.gold, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            if (isTempFile) {
                TextButton(onClick = onDismiss) {
                    Text("Ignorer", color = tokens.textMid, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
                }
            }
        },
    )
}
