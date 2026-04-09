package com.mazzika.lyrics.ui.sync

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.data.nearby.NearbySessionManager
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.mazzika.lyrics.ui.theme.CormorantGaramond
import com.mazzika.lyrics.ui.theme.DarkBackground
import com.mazzika.lyrics.ui.theme.DarkSurface
import com.mazzika.lyrics.ui.theme.DarkSurfaceElevated
import com.mazzika.lyrics.ui.theme.DarkTextMuted
import com.mazzika.lyrics.ui.theme.DarkTextPrimary
import com.mazzika.lyrics.ui.theme.DarkTextSecondary
import com.mazzika.lyrics.ui.theme.Error
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.GoldDark
import com.mazzika.lyrics.ui.theme.GoldDeep
import com.mazzika.lyrics.ui.theme.Success

@Composable
fun SyncScreen(
    onNavigateToReaderSync: () -> Unit,
    onNavigateToReader: (Long) -> Unit,
    viewModel: SyncViewModel = viewModel(),
) {
    val context = LocalContext.current
    val role by viewModel.role.collectAsState()
    val selectedDocument by viewModel.selectedDocument.collectAsState()
    val syncFilePath by viewModel.syncFilePath.collectAsState()
    val allDocuments by viewModel.allDocuments.collectAsState()
    val allFolders by viewModel.allFolders.collectAsState()
    val connectedEndpoints by viewModel.connectedEndpoints.collectAsState()
    val discoveredEndpoints by viewModel.discoveredEndpoints.collectAsState()
    val sessionName by viewModel.sessionName.collectAsState()
    val transferProgress by viewModel.transferProgress.collectAsState()
    val sessionEndedByPilot by viewModel.sessionEndedByPilot.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val discoveryTimedOut by viewModel.discoveryTimedOut.collectAsState()
    val isTempFile by viewModel.isTempFile.collectAsState()

    // Build required permissions based on API level
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
            // Nearby Connections needs FINE_LOCATION on all API levels
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            pendingAction?.invoke()
        }
        pendingAction = null
    }

    fun requestPermissionsAndRun(action: () -> Unit) {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            action()
        } else {
            pendingAction = action
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    // Navigate to reader when a file is ready (only once per session)
    LaunchedEffect(syncFilePath) {
        if (syncFilePath != null && !viewModel.hasNavigatedToReader) {
            viewModel.markNavigatedToReader()
            onNavigateToReaderSync()
        }
    }

    // Dialog states
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    // Session ended by pilot dialog
    if (sessionEndedByPilot) {
        SessionEndedDialog(
            isTempFile = isTempFile,
            onSave = {
                viewModel.saveToCatalogue()
            },
            onDismiss = {
                viewModel.acknowledgeSessionEnd()
                viewModel.stopSession()
            },
        )
    }

    Scaffold(
        containerColor = DarkBackground,
    ) { innerPadding ->
        when (role) {
            SyncRole.NONE -> {
                NoneState(
                    innerPadding = innerPadding,
                    onCreateSession = {
                        requestPermissionsAndRun {
                            viewModel.initSessionName()
                            showCreateDialog = true
                        }
                    },
                    onJoinSession = {
                        requestPermissionsAndRun {
                            viewModel.startAsFollower()
                            showJoinDialog = true
                        }
                    },
                )
            }
            SyncRole.PILOT -> PilotState(
                sessionName = sessionName,
                selectedDocument = selectedDocument,
                connectedEndpoints = connectedEndpoints,
                innerPadding = innerPadding,
                onOpenReader = {
                    selectedDocument?.let { doc -> onNavigateToReader(doc.id) }
                },
                onStop = { viewModel.stopSession() },
            )
            SyncRole.FOLLOWER -> {
                if (!showJoinDialog) {
                    FollowerActiveState(
                        sessionName = sessionName,
                        selectedDocument = selectedDocument,
                        connectedEndpoints = connectedEndpoints,
                        innerPadding = innerPadding,
                        onOpenReader = { onNavigateToReaderSync() },
                        onLeave = { viewModel.stopSession() },
                    )
                }
            }
        }
    }

    // Create session stepper dialog
    if (showCreateDialog) {
        CreateSessionDialog(
            sessionName = sessionName,
            onSessionNameChange = { viewModel.setSessionName(it) },
            allDocuments = allDocuments,
            allFolders = allFolders,
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false },
            onStart = { doc ->
                viewModel.startAsPilot(doc)
                showCreateDialog = false
            },
        )
    }

    // Join session dialog
    if (showJoinDialog && role == SyncRole.FOLLOWER) {
        JoinSessionDialog(
            discoveredEndpoints = discoveredEndpoints,
            isSearching = isSearching,
            discoveryTimedOut = discoveryTimedOut,
            transferProgress = transferProgress,
            onJoin = { endpointId ->
                viewModel.connectToEndpoint(endpointId)
                showJoinDialog = false
            },
            onRestart = { viewModel.restartDiscovery() },
            onDismiss = {
                showJoinDialog = false
                if (connectedEndpoints.isEmpty()) {
                    viewModel.stopSession()
                }
            },
        )
    }
}

// ============================================================================
// NONE state — Two cards only
// ============================================================================

// Teal soft gradient for create card
private val TealCardBg = Brush.verticalGradient(
    listOf(Color(0x1F5DB8A9), Color(0x085DB8A9))
)
private val TealIconGradient = Brush.linearGradient(
    listOf(Color(0xFF5DB8A9), Color(0xFF3D9B8F))
)

// Blue soft gradient for join card
private val BlueCardBg = Brush.verticalGradient(
    listOf(Color(0x1F6BA3D4), Color(0x086BA3D4))
)
private val BlueIconGradient = Brush.linearGradient(
    listOf(Color(0xFF6BA3D4), Color(0xFF4A82B8))
)

@Composable
private fun NoneState(
    innerPadding: PaddingValues,
    onCreateSession: () -> Unit,
    onJoinSession: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero section
        Text(
            text = "Synchronisation",
            fontFamily = CormorantGaramond,
            color = DarkTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Partagez vos partitions en temps r\u00E9el",
            color = DarkTextMuted,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Create session card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(TealCardBg)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .clickable { onCreateSession() }
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Icon square 56dp with teal gradient
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(TealIconGradient),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "\uD83D\uDCE1", // 📡
                            fontSize = 24.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Cr\u00E9er une session",
                        color = DarkTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Diffusez une partition \u00E0 votre \u00E9quipe",
                        color = DarkTextMuted,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Join session card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(BlueCardBg)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .clickable { onJoinSession() }
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Icon square 56dp with blue gradient
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BlueIconGradient),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "\uD83D\uDD0D", // 🔍
                            fontSize = 24.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Rejoindre",
                        color = DarkTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rechercher les sessions \u00E0 proximit\u00E9",
                        color = DarkTextMuted,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ============================================================================
// Create Session Stepper Dialog
// ============================================================================

@Composable
private fun CreateSessionDialog(
    sessionName: String,
    onSessionNameChange: (String) -> Unit,
    allDocuments: List<PdfDocumentEntity>,
    allFolders: List<FolderEntity>,
    viewModel: SyncViewModel,
    onDismiss: () -> Unit,
    onStart: (PdfDocumentEntity) -> Unit,
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var selectedDoc by remember { mutableStateOf<PdfDocumentEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 24.dp),
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "Créer une session",
                    color = DarkTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Step indicator
                StepIndicator(currentStep = currentStep, totalSteps = 3)
            }
        },
        text = {
            when (currentStep) {
                0 -> StepSessionName(
                    sessionName = sessionName,
                    onSessionNameChange = onSessionNameChange,
                )
                1 -> StepFilePicker(
                    allDocuments = allDocuments,
                    allFolders = allFolders,
                    viewModel = viewModel,
                    selectedDoc = selectedDoc,
                    onSelectDoc = { selectedDoc = it },
                )
                2 -> StepConfirm(
                    sessionName = sessionName,
                    selectedDoc = selectedDoc,
                )
            }
        },
        confirmButton = {
            Row {
                if (currentStep > 0) {
                    TextButton(onClick = { currentStep-- }) {
                        Text("Précédent", color = DarkTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                when (currentStep) {
                    0 -> Button(
                        onClick = { currentStep = 1 },
                        enabled = sessionName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = DarkBackground,
                            disabledContainerColor = GoldDark.copy(alpha = 0.3f),
                            disabledContentColor = DarkTextMuted,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Suivant")
                    }
                    1 -> Button(
                        onClick = { currentStep = 2 },
                        enabled = selectedDoc != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = DarkBackground,
                            disabledContainerColor = GoldDark.copy(alpha = 0.3f),
                            disabledContentColor = DarkTextMuted,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Suivant")
                    }
                    2 -> Button(
                        onClick = { selectedDoc?.let { onStart(it) } },
                        enabled = sessionName.isNotBlank() && selectedDoc != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = DarkBackground,
                            disabledContainerColor = GoldDark.copy(alpha = 0.3f),
                            disabledContentColor = DarkTextMuted,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CellTower,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Démarrer la session")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = DarkTextMuted)
            }
        },
    )
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(totalSteps) { index ->
            val isActive = index <= currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        color = if (isActive) Gold else DarkSurfaceElevated,
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

@Composable
private fun StepSessionName(
    sessionName: String,
    onSessionNameChange: (String) -> Unit,
) {
    Column {
        Text(
            text = "Nom de la session",
            color = DarkTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = sessionName,
            onValueChange = onSessionNameChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DarkTextPrimary,
                unfocusedTextColor = DarkTextPrimary,
                cursorColor = Gold,
                focusedBorderColor = Gold,
                unfocusedBorderColor = DarkSurfaceElevated,
                focusedContainerColor = DarkBackground,
                unfocusedContainerColor = DarkBackground,
            ),
            shape = RoundedCornerShape(10.dp),
            placeholder = {
                Text("Nom de la session", color = DarkTextMuted)
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ce nom sera visible par les autres appareils",
            color = DarkTextMuted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun StepFilePicker(
    allDocuments: List<PdfDocumentEntity>,
    allFolders: List<FolderEntity>,
    viewModel: SyncViewModel,
    selectedDoc: PdfDocumentEntity?,
    onSelectDoc: (PdfDocumentEntity) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Catalogue", "Dossiers")

    Column(modifier = Modifier.height(350.dp)) {
        Text(
            text = "Choisir une partition",
            color = DarkTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(8.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkBackground,
            contentColor = Gold,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Gold,
                    )
                }
            },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) Gold else DarkTextMuted,
                            fontSize = 13.sp,
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> {
                // Catalogue tab - all documents
                if (allDocuments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Aucune partition dans le catalogue",
                            color = DarkTextMuted,
                            fontSize = 14.sp,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(allDocuments) { doc ->
                            FilePickerItem(
                                document = doc,
                                isSelected = selectedDoc?.id == doc.id,
                                onClick = { onSelectDoc(doc) },
                            )
                        }
                    }
                }
            }
            1 -> {
                // Folders tab
                if (allFolders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Aucun dossier",
                            color = DarkTextMuted,
                            fontSize = 14.sp,
                        )
                    }
                } else {
                    var expandedFolderId by remember { mutableStateOf<Long?>(null) }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        allFolders.forEach { folder ->
                            item(key = "folder_${folder.id}") {
                                FolderPickerItem(
                                    folder = folder,
                                    isExpanded = expandedFolderId == folder.id,
                                    onClick = {
                                        expandedFolderId = if (expandedFolderId == folder.id) null else folder.id
                                    },
                                )
                            }
                            if (expandedFolderId == folder.id) {
                                item(key = "folder_docs_${folder.id}") {
                                    FolderDocumentsList(
                                        folderId = folder.id,
                                        viewModel = viewModel,
                                        selectedDoc = selectedDoc,
                                        onSelectDoc = onSelectDoc,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderDocumentsList(
    folderId: Long,
    viewModel: SyncViewModel,
    selectedDoc: PdfDocumentEntity?,
    onSelectDoc: (PdfDocumentEntity) -> Unit,
) {
    val docs by viewModel.getDocumentsInFolder(folderId)
        .collectAsState(initial = emptyList())

    if (docs.isEmpty()) {
        Text(
            text = "Aucune partition dans ce dossier",
            color = DarkTextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 32.dp, top = 4.dp, bottom = 4.dp),
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            docs.forEach { doc ->
                FilePickerItem(
                    document = doc,
                    isSelected = selectedDoc?.id == doc.id,
                    onClick = { onSelectDoc(doc) },
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun FilePickerItem(
    document: PdfDocumentEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isSelected) GoldDeep.copy(alpha = 0.3f) else DarkBackground
    val borderColor = if (isSelected) Gold else Color.Transparent

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = if (isSelected) BorderStroke(1.dp, borderColor) else null,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = if (isSelected) Gold else DarkTextMuted,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            ) {
                Text(
                    text = document.title,
                    color = DarkTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${document.pageCount} pages",
                    color = DarkTextMuted,
                    fontSize = 11.sp,
                )
            }
            if (isSelected) {
                Text(
                    text = "Sélectionné",
                    color = Gold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun FolderPickerItem(
    folder: FolderEntity,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) DarkSurfaceElevated else DarkBackground,
        ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = folder.name,
                color = DarkTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (isExpanded) "Fermer" else "Ouvrir",
                color = DarkTextMuted,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun StepConfirm(
    sessionName: String,
    selectedDoc: PdfDocumentEntity?,
) {
    Column {
        Text(
            text = "Résumé",
            color = DarkTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkBackground),
            shape = RoundedCornerShape(10.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CellTower,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sessionName,
                        color = DarkTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (selectedDoc != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            tint = DarkTextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = selectedDoc.title,
                                color = DarkTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "${selectedDoc.pageCount} pages",
                                color = DarkTextMuted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "La session démarrera immédiatement et sera visible par les appareils à proximité.",
            color = DarkTextMuted,
            fontSize = 12.sp,
        )
    }
}

// ============================================================================
// Join Session Dialog
// ============================================================================

@Composable
private fun JoinSessionDialog(
    discoveredEndpoints: List<NearbySessionManager.EndpointInfo>,
    isSearching: Boolean,
    discoveryTimedOut: Boolean,
    transferProgress: Float?,
    onJoin: (String) -> Unit,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 24.dp),
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Rejoindre une session",
                color = DarkTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.height(300.dp)) {
                // Transfer in progress
                if (transferProgress != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Transfert en cours...",
                            color = DarkTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { transferProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = Gold,
                            trackColor = DarkSurfaceElevated,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(transferProgress * 100).toInt()}%",
                            color = Gold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    return@Column
                }

                // Central zone: searching OR no results OR sessions list
                if (discoveredEndpoints.isEmpty()) {
                    // Centered content: loading or "no sessions"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isSearching) {
                                // Loading state: spinner + text
                                CircularProgressIndicator(
                                    color = Gold,
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 3.dp,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Recherche de sessions...",
                                    color = DarkTextSecondary,
                                    fontSize = 14.sp,
                                )
                            } else if (discoveryTimedOut) {
                                // No results: icon + text
                                Icon(
                                    imageVector = Icons.Filled.Wifi,
                                    contentDescription = null,
                                    tint = DarkTextMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Aucune session trouvée",
                                    color = DarkTextSecondary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Vérifiez que le chef de pupitre diffuse une session",
                                    color = DarkTextMuted,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            } else {
                                Text(
                                    text = "Appuyez sur Relancer pour chercher",
                                    color = DarkTextMuted,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                } else {
                    // Searching indicator when sessions already found
                    if (isSearching) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(
                                color = Gold,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = "Recherche en cours...",
                                color = DarkTextMuted,
                                fontSize = 12.sp,
                            )
                        }
                    }

                    // Discovered sessions list
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(discoveredEndpoints) { endpoint ->
                            DiscoveredEndpointItem(
                                endpoint = endpoint,
                                onJoin = { onJoin(endpoint.id) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Relancer button (right of Annuler)
            TextButton(
                onClick = onRestart,
                enabled = !isSearching,
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = if (!isSearching) Gold else DarkTextMuted,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Relancer",
                    color = if (!isSearching) Gold else DarkTextMuted,
                    fontSize = 13.sp,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = DarkTextMuted)
            }
        },
    )
}

@Composable
private fun DiscoveredEndpointItem(
    endpoint: NearbySessionManager.EndpointInfo,
    onJoin: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkBackground),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
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
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column {
                    Text(
                        text = endpoint.name,
                        color = DarkTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
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

// ============================================================================
// PILOT state (session active)
// ============================================================================

@Composable
private fun PilotState(
    sessionName: String,
    selectedDocument: PdfDocumentEntity?,
    connectedEndpoints: List<NearbySessionManager.EndpointInfo>,
    innerPadding: PaddingValues,
    onOpenReader: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(20.dp),
    ) {
        // Active session card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Success)
                        }
                    }
                    Text(
                        text = "Session active",
                        color = Success,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (sessionName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = sessionName,
                        color = DarkTextSecondary,
                        fontSize = 13.sp,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedDocument != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = selectedDocument.title,
                                color = DarkTextPrimary,
                                fontSize = 16.sp,
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
                    }
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
                            text = "  ${endpoint.name}",
                            color = DarkTextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Open reader button
        Button(
            onClick = onOpenReader,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor = DarkBackground,
            ),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp),
            )
            Text("Ouvrir le lecteur", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
            Text("Arrêter la diffusion", fontSize = 15.sp)
        }
    }
}

// ============================================================================
// FOLLOWER state (joined session, dialog dismissed)
// ============================================================================

@Composable
private fun FollowerActiveState(
    sessionName: String,
    selectedDocument: PdfDocumentEntity?,
    connectedEndpoints: List<NearbySessionManager.EndpointInfo>,
    innerPadding: PaddingValues,
    onOpenReader: () -> Unit,
    onLeave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(20.dp),
    ) {
        // Session info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Success)
                        }
                    }
                    Text(
                        text = "Connecté",
                        color = Success,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (sessionName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = sessionName,
                        color = DarkTextSecondary,
                        fontSize = 13.sp,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedDocument != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = selectedDocument.title,
                                color = DarkTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

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
                        text = "${connectedEndpoints.size} appareil(s) dans la session",
                        color = DarkTextSecondary,
                        fontSize = 14.sp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Open reader button
        Button(
            onClick = onOpenReader,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor = DarkBackground,
            ),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp),
            )
            Text("Ouvrir le lecteur", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Leave session button
        OutlinedButton(
            onClick = onLeave,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
            border = BorderStroke(1.dp, Error),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp),
            )
            Text("Quitter la session", fontSize = 15.sp)
        }
    }
}

// ============================================================================
// Session Ended Dialog (shown to follower when pilot ends session)
// ============================================================================

@Composable
private fun SessionEndedDialog(
    isTempFile: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Session terminée",
                color = DarkTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = "Le chef de pupitre a mis fin à la session.",
                color = DarkTextSecondary,
                fontSize = 14.sp,
            )
        },
        confirmButton = {
            if (isTempFile) {
                Button(
                    onClick = {
                        onSave()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = DarkBackground,
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sauvegarder le fichier")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = DarkTextMuted)
            }
        },
    )
}
