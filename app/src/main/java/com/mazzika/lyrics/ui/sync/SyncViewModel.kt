package com.mazzika.lyrics.ui.sync

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mazzika.lyrics.MazzikaApplication
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.data.nearby.NearbyService
import com.mazzika.lyrics.data.nearby.NearbySessionManager
import com.mazzika.lyrics.data.nearby.SyncMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class SyncRole { NONE, PILOT, FOLLOWER }

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SyncViewModel"
        const val DISCOVERY_TIMEOUT_SECONDS = 15
    }

    private val app = application as MazzikaApplication

    private val _role = MutableStateFlow(SyncRole.NONE)
    val role: StateFlow<SyncRole> = _role.asStateFlow()

    private val _selectedDocument = MutableStateFlow<PdfDocumentEntity?>(null)
    val selectedDocument: StateFlow<PdfDocumentEntity?> = _selectedDocument.asStateFlow()

    private val _syncFilePath = MutableStateFlow<String?>(null)
    val syncFilePath: StateFlow<String?> = _syncFilePath.asStateFlow()

    /** True when the synced file is stored as a temp file (not yet saved to catalogue). */
    private val _isTempFile = MutableStateFlow(false)
    val isTempFile: StateFlow<Boolean> = _isTempFile.asStateFlow()

    /** Page number sent by the pilot; observed by the follower's ReaderScreen. */
    private val _syncPage = MutableStateFlow<Int?>(null)
    val syncPage: StateFlow<Int?> = _syncPage.asStateFlow()

    /** Always tracks the latest page from the pilot, even when detached. */
    private val _pilotCurrentPage = MutableStateFlow(0)
    val pilotCurrentPage: StateFlow<Int> = _pilotCurrentPage.asStateFlow()

    /** When true the follower navigates freely; PAGE_CHANGE is recorded but not applied. */
    private val _isDetached = MutableStateFlow(false)
    val isDetached: StateFlow<Boolean> = _isDetached.asStateFlow()

    // --- New session management features ---

    private val _sessionName = MutableStateFlow("")
    val sessionName: StateFlow<String> = _sessionName.asStateFlow()

    private val _transferProgress = MutableStateFlow<Float?>(null)
    val transferProgress: StateFlow<Float?> = _transferProgress.asStateFlow()

    private val _sessionEndedByPilot = MutableStateFlow(false)
    val sessionEndedByPilot: StateFlow<Boolean> = _sessionEndedByPilot.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _discoveryTimedOut = MutableStateFlow(false)
    val discoveryTimedOut: StateFlow<Boolean> = _discoveryTimedOut.asStateFlow()

    private var discoveryTimeoutJob: Job? = null

    // Follower-side: original title/fileName advertised by the pilot in SessionInfo.
    // Used to rename the received temp file and to populate the catalog entry.
    private var pendingTitle: String? = null
    private var pendingFileName: String? = null

    fun setSessionName(name: String) {
        _sessionName.value = name
    }

    /** Call after navigating to reader to prevent re-navigation on recomposition. */
    fun markNavigatedToReader() {
        _navigatedToReader = true
    }

    private var _navigatedToReader = false
    val hasNavigatedToReader: Boolean get() = _navigatedToReader

    fun acknowledgeSessionEnd() {
        _sessionEndedByPilot.value = false
    }

    val allDocuments: StateFlow<List<PdfDocumentEntity>> = app.database.pdfDocumentDao()
        .getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val allFolders: StateFlow<List<FolderEntity>> = app.database.folderDao()
        .getRootFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _sessionManager = MutableStateFlow<NearbySessionManager?>(null)

    val connectedEndpoints: StateFlow<List<NearbySessionManager.EndpointInfo>> =
        _sessionManager.flatMapLatest { manager ->
            manager?.connectedEndpoints ?: flowOf(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val discoveredEndpoints: StateFlow<List<NearbySessionManager.EndpointInfo>> =
        _sessionManager.flatMapLatest { manager ->
            manager?.discoveredEndpoints ?: flowOf(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private var isBound = false
    private var serviceBinder: com.mazzika.lyrics.data.nearby.NearbyService.LocalBinder? = null
    private val _serviceReady = MutableStateFlow(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as NearbyService.LocalBinder
            serviceBinder = binder
            _sessionManager.value = binder.getSessionManager()
            isBound = true
            _serviceReady.value = true
            observeIncomingMessages()
            observeIncomingFiles()
            observeIncomingFilePaths()
            observeConnectedEndpoints()
            observeTransferProgress()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBinder = null
            _sessionManager.value = null
            isBound = false
            _serviceReady.value = false
        }
    }

    private suspend fun awaitServiceReady() {
        if (_serviceReady.value) return
        _serviceReady.first { it }
    }

    private fun startService() {
        if (isBound) return
        val intent = Intent(app, NearbyService::class.java)
        app.startForegroundService(intent)
        app.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopService() {
        if (!isBound) return
        // Kill the foreground notification *before* unbinding so it's never stranded
        // on screen if the platform delays onDestroy.
        runCatching { serviceBinder?.tearDownForeground() }
        app.unbindService(serviceConnection)
        app.stopService(Intent(app, NearbyService::class.java))
        serviceBinder = null
        _sessionManager.value = null
        isBound = false
        _serviceReady.value = false
    }

    private fun observeIncomingMessages() {
        _sessionManager.value?.incomingMessages?.onEach { pair ->
            pair ?: return@onEach
            val (endpointId, message) = pair
            Log.d(TAG, "Received message from $endpointId: $message")
            handleMessage(endpointId, message)
        }?.launchIn(viewModelScope)
    }

    private fun observeIncomingFiles() {
        _sessionManager.value?.incomingFile?.onEach { pair ->
            pair ?: return@onEach
            val (_, bytes) = pair
            handleIncomingFile(bytes)
        }?.launchIn(viewModelScope)
    }

    private fun observeIncomingFilePaths() {
        _sessionManager.value?.incomingFilePath?.onEach { pair ->
            pair ?: return@onEach
            val (_, path) = pair
            handleIncomingFilePath(path)
        }?.launchIn(viewModelScope)
    }

    private fun observeTransferProgress() {
        _sessionManager.value?.transferProgress?.onEach { progress ->
            _transferProgress.value = progress
        }?.launchIn(viewModelScope)
    }

    private fun handleMessage(endpointId: String, message: SyncMessage) {
        viewModelScope.launch {
            val manager = _sessionManager.value ?: run {
                Log.e(TAG, "handleMessage: sessionManager is null!")
                return@launch
            }
            when (message) {
                is SyncMessage.SessionInfo -> {
                    Log.d(TAG, "SessionInfo received: title=${message.title}, fileName=${message.fileName}, hash=${message.fileHash}")
                    // Remember the original title/filename so we can name the temp file
                    // correctly when the payload arrives (and later when saving to catalog).
                    pendingTitle = message.title
                    pendingFileName = message.fileName
                    val existing = app.database.pdfDocumentDao().getByHash(message.fileHash)
                    if (existing != null) {
                        Log.d(TAG, "File already exists locally at ${existing.filePath}")
                        manager.sendMessage(endpointId, SyncMessage.AlreadyHave(message.fileHash))
                        _syncFilePath.value = existing.filePath
                    } else {
                        Log.d(TAG, "File not found locally, requesting transfer")
                        manager.sendMessage(endpointId, SyncMessage.NeedFile(message.fileHash))
                    }
                }
                is SyncMessage.NeedFile -> {
                    Log.d(TAG, "NeedFile received for hash=${message.fileHash}")
                    val doc = app.database.pdfDocumentDao().getByHash(message.fileHash)
                    if (doc != null) {
                        Log.d(TAG, "Sending file: ${doc.filePath}")
                        manager.sendFile(endpointId, doc.filePath)
                    } else {
                        Log.e(TAG, "Cannot find file with hash=${message.fileHash} to send!")
                    }
                }
                is SyncMessage.AlreadyHave -> {
                    Log.d(TAG, "Follower already has the file")
                }
                is SyncMessage.PageChange -> {
                    Log.d(TAG, "PageChange to ${message.page}, detached=${_isDetached.value}")
                    _pilotCurrentPage.value = message.page
                    if (!_isDetached.value) {
                        _syncPage.value = message.page
                    }
                }
                is SyncMessage.SessionEnd -> {
                    Log.d(TAG, "SessionEnd received: reason=${message.reason}")
                    // Keep syncFilePath/isTempFile/sessionEndedByPilot so the user can
                    // still save the temp file; tear down nearby + stop the foreground
                    // service so the "Session Mazzika active" notification disappears.
                    _sessionEndedByPilot.value = true
                    _role.value = SyncRole.NONE
                    _sessionManager.value?.run {
                        stopAdvertising()
                        stopDiscovery()
                        disconnectAll()
                    }
                    stopService()
                }
            }
        }
    }

    private fun handleIncomingFile(bytes: ByteArray) {
        viewModelScope.launch {
            val baseName = pendingFileName ?: "sync_received.pdf"
            val tempPath = app.fileManager.saveTempFile(bytes, baseName)
            val autoSave = app.userPreferences.autoSaveSync
                .stateIn(viewModelScope, SharingStarted.Eagerly, false).value

            if (autoSave) {
                val finalPath = app.fileManager.moveTempToCatalog(tempPath)
                _syncFilePath.value = finalPath
                _isTempFile.value = false
            } else {
                _syncFilePath.value = tempPath
                _isTempFile.value = true
            }
        }
    }

    /** Called when a FILE payload is received (large PDF). */
    fun handleIncomingFilePath(path: String) {
        Log.d(TAG, "handleIncomingFilePath: $path")
        viewModelScope.launch {
            // Rename the incoming temp file to reflect the original filename advertised
            // by the pilot. The transport-layer placeholder name ("sync_received.pdf")
            // is replaced while preserving the existing timestamp prefix so names stay
            // unique on disk.
            val effectivePath = pendingFileName?.let { renameTempFile(path, it) } ?: path

            val autoSave = app.userPreferences.autoSaveSync
                .stateIn(viewModelScope, SharingStarted.Eagerly, false).value

            if (autoSave) {
                val finalPath = app.fileManager.moveTempToCatalog(effectivePath)
                _syncFilePath.value = finalPath
                _isTempFile.value = false
            } else {
                _syncFilePath.value = effectivePath
                _isTempFile.value = true
            }
        }
    }

    /**
     * Rename a temp file from `<timestamp>_sync_received.pdf` (or similar) to
     * `<timestamp>_<originalFileName>`. Returns the new path, or the original path
     * if the rename failed.
     */
    private fun renameTempFile(originalPath: String, originalFileName: String): String {
        return try {
            val oldFile = java.io.File(originalPath)
            if (!oldFile.exists()) return originalPath
            val sanitized = originalFileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val ensured = if (sanitized.endsWith(".pdf", ignoreCase = true)) sanitized else "$sanitized.pdf"
            // Preserve the timestamp prefix if there is one so concurrent receptions
            // from the same pilot keep distinct filenames on disk.
            val oldName = oldFile.name
            val prefix = oldName.substringBefore('_', missingDelimiterValue = "").let {
                if (it.toLongOrNull() != null) "${it}_" else "${System.currentTimeMillis()}_"
            }
            val newFile = java.io.File(oldFile.parent, "$prefix$ensured")
            if (oldFile.renameTo(newFile)) {
                Log.d(TAG, "Renamed temp file → ${newFile.absolutePath}")
                newFile.absolutePath
            } else {
                originalPath
            }
        } catch (e: Exception) {
            Log.e(TAG, "renameTempFile failed", e)
            originalPath
        }
    }

    /** Move the temp synced file to the permanent catalogue and insert a DB record. */
    fun saveToCatalogue() {
        val currentPath = _syncFilePath.value ?: return
        if (!_isTempFile.value) return
        viewModelScope.launch {
            val finalPath = app.fileManager.moveTempToCatalog(currentPath)
            val file = java.io.File(finalPath)
            val hash = app.fileManager.computeHash(file)
            val pageCount = app.fileManager.getPageCount(file)
            val thumbnailPath = app.fileManager.generateThumbnail(file, hash)
            // Prefer the title/filename sent by the pilot; fall back to the on-disk
            // name (minus the timestamp prefix added by FileManager) otherwise.
            val originalFileName = pendingFileName
                ?: file.name.substringAfter('_', missingDelimiterValue = file.name)
            val originalTitle = pendingTitle
                ?: originalFileName.substringBeforeLast('.')
            val entity = com.mazzika.lyrics.data.db.entity.PdfDocumentEntity(
                title = originalTitle,
                fileName = originalFileName,
                filePath = finalPath,
                fileHash = hash,
                pageCount = pageCount,
                thumbnailPath = thumbnailPath,
                importedAt = System.currentTimeMillis(),
            )
            app.database.pdfDocumentDao().insert(entity)
            _syncFilePath.value = finalPath
            _isTempFile.value = false
        }
    }

    /** Toggle detached mode. When re-attaching, jump to the pilot's current page. */
    fun toggleDetached() {
        val wasDetached = _isDetached.value
        _isDetached.value = !wasDetached
        if (wasDetached) {
            // Re-synchronise: force syncPage update even if same value
            _syncPage.value = null
            _syncPage.value = _pilotCurrentPage.value
        }
    }

    fun startAsPilot(document: PdfDocumentEntity) {
        Log.d(TAG, "startAsPilot: ${document.title}")
        _selectedDocument.value = document
        _role.value = SyncRole.PILOT
        startService()
        viewModelScope.launch {
            awaitServiceReady()
            val deviceName = app.userPreferences.deviceName.first()
            if (_sessionName.value.isBlank()) {
                _sessionName.value = "Session de $deviceName"
            }
            Log.d(TAG, "Starting advertising as '$deviceName'")
            _sessionManager.value?.startAdvertising(deviceName)
        }
    }

    fun startAsFollower() {
        Log.d(TAG, "startAsFollower")
        _role.value = SyncRole.FOLLOWER
        startService()
        viewModelScope.launch {
            awaitServiceReady()
            Log.d(TAG, "Starting discovery")
            startDiscoveryWithTimeout()
        }
    }

    fun startDiscoveryWithTimeout() {
        _isSearching.value = true
        _discoveryTimedOut.value = false
        _sessionManager.value?.startDiscovery()
        discoveryTimeoutJob?.cancel()
        discoveryTimeoutJob = viewModelScope.launch {
            delay(DISCOVERY_TIMEOUT_SECONDS * 1000L)
            _isSearching.value = false
            _discoveryTimedOut.value = true
            // Stop listening once the timeout elapses — resumes on explicit retry.
            _sessionManager.value?.stopDiscovery()
        }
    }

    fun restartDiscovery() {
        discoveryTimeoutJob?.cancel()
        _sessionManager.value?.stopDiscovery()
        _sessionManager.value?.clearDiscoveredEndpoints()
        startDiscoveryWithTimeout()
    }

    fun connectToEndpoint(endpointId: String) {
        viewModelScope.launch {
            awaitServiceReady()
            val deviceName = app.userPreferences.deviceName.first()
            Log.d(TAG, "Requesting connection to $endpointId as '$deviceName'")
            _sessionManager.value?.requestConnection(endpointId, deviceName)
        }
    }

    fun broadcastPageChange(page: Int) {
        _sessionManager.value?.broadcastMessage(SyncMessage.PageChange(page))
    }

    private fun observeConnectedEndpoints() {
        _sessionManager.value?.connectedEndpoints?.onEach { endpoints ->
            Log.d(TAG, "Connected endpoints changed: ${endpoints.size} endpoints, role=${_role.value}")
            if (_role.value == SyncRole.PILOT) {
                val doc = _selectedDocument.value ?: run {
                    Log.w(TAG, "No selected document to share")
                    return@onEach
                }
                val message = SyncMessage.SessionInfo(
                    title = doc.title,
                    fileName = doc.fileName,
                    pageCount = doc.pageCount,
                    fileHash = doc.fileHash,
                )
                endpoints.forEach { endpoint ->
                    Log.d(TAG, "Sending SESSION_INFO to ${endpoint.id} (${endpoint.name})")
                    _sessionManager.value?.sendMessage(endpoint.id, message)
                }
            }
        }?.launchIn(viewModelScope)
    }

    fun stopSession() {
        // If pilot, send SESSION_END to all followers before disconnecting
        if (_role.value == SyncRole.PILOT) {
            _sessionManager.value?.broadcastMessage(
                SyncMessage.SessionEnd(reason = "Le chef de pupitre a arrêté la session"),
            )
        }
        discoveryTimeoutJob?.cancel()
        _sessionManager.value?.run {
            stopAdvertising()
            stopDiscovery()
            disconnectAll()
        }
        stopService()
        _role.value = SyncRole.NONE
        _selectedDocument.value = null
        _syncFilePath.value = null
        _syncPage.value = null
        _isDetached.value = false
        _pilotCurrentPage.value = 0
        _isTempFile.value = false
        _sessionName.value = ""
        _transferProgress.value = null
        _isSearching.value = false
        _discoveryTimedOut.value = false
        pendingTitle = null
        pendingFileName = null
        _navigatedToReader = false
    }

    /** Initialize default session name from device name. */
    fun initSessionName() {
        if (_sessionName.value.isBlank()) {
            viewModelScope.launch {
                val deviceName = app.userPreferences.deviceName.first()
                _sessionName.value = "Session de $deviceName"
            }
        }
    }

    fun getDocumentsInFolder(folderId: Long) =
        app.database.folderDocumentRefDao().getDocumentsInFolder(folderId)

    override fun onCleared() {
        super.onCleared()
        discoveryTimeoutJob?.cancel()
        stopService()
    }
}
