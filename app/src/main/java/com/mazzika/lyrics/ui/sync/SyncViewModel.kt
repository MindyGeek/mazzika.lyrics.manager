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
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.data.nearby.NearbyService
import com.mazzika.lyrics.data.nearby.NearbySessionManager
import com.mazzika.lyrics.data.nearby.SyncMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    val allDocuments: StateFlow<List<PdfDocumentEntity>> = app.database.pdfDocumentDao()
        .getAll()
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
    private val _serviceReady = MutableStateFlow(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as NearbyService.LocalBinder
            _sessionManager.value = binder.getSessionManager()
            isBound = true
            _serviceReady.value = true
            observeIncomingMessages()
            observeIncomingFiles()
            observeIncomingFilePaths()
            observeConnectedEndpoints()
        }

        override fun onServiceDisconnected(name: ComponentName) {
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
        app.unbindService(serviceConnection)
        app.stopService(Intent(app, NearbyService::class.java))
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

    private fun handleMessage(endpointId: String, message: SyncMessage) {
        viewModelScope.launch {
            val manager = _sessionManager.value ?: run {
                Log.e(TAG, "handleMessage: sessionManager is null!")
                return@launch
            }
            when (message) {
                is SyncMessage.SessionInfo -> {
                    Log.d(TAG, "SessionInfo received: title=${message.title}, hash=${message.fileHash}")
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
            }
        }
    }

    private fun handleIncomingFile(bytes: ByteArray) {
        viewModelScope.launch {
            val tempPath = app.fileManager.saveTempFile(bytes, "sync_received.pdf")
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
            val autoSave = app.userPreferences.autoSaveSync
                .stateIn(viewModelScope, SharingStarted.Eagerly, false).value

            if (autoSave) {
                val finalPath = app.fileManager.moveTempToCatalog(path)
                _syncFilePath.value = finalPath
                _isTempFile.value = false
            } else {
                _syncFilePath.value = path
                _isTempFile.value = true
            }
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
            val entity = com.mazzika.lyrics.data.db.entity.PdfDocumentEntity(
                title = file.nameWithoutExtension,
                fileName = file.name,
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
            // Re-synchronise: apply the latest pilot page
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
            _sessionManager.value?.startDiscovery()
        }
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
    }

    override fun onCleared() {
        super.onCleared()
        stopService()
    }
}
