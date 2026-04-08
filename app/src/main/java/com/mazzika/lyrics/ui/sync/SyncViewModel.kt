package com.mazzika.lyrics.ui.sync

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
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

    private val app = application as MazzikaApplication

    private val _role = MutableStateFlow(SyncRole.NONE)
    val role: StateFlow<SyncRole> = _role.asStateFlow()

    private val _selectedDocument = MutableStateFlow<PdfDocumentEntity?>(null)
    val selectedDocument: StateFlow<PdfDocumentEntity?> = _selectedDocument.asStateFlow()

    private val _syncFilePath = MutableStateFlow<String?>(null)
    val syncFilePath: StateFlow<String?> = _syncFilePath.asStateFlow()

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

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as NearbyService.LocalBinder
            _sessionManager.value = binder.getSessionManager()
            isBound = true
            observeIncomingMessages()
            observeIncomingFiles()
            observeIncomingFilePaths()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            _sessionManager.value = null
            isBound = false
        }
    }

    init {
        val intent = Intent(application, NearbyService::class.java)
        application.startForegroundService(intent)
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun observeIncomingMessages() {
        _sessionManager.value?.incomingMessages?.onEach { pair ->
            pair ?: return@onEach
            val (endpointId, message) = pair
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
            val manager = _sessionManager.value ?: return@launch
            when (message) {
                is SyncMessage.SessionInfo -> {
                    val existing = app.database.pdfDocumentDao().getByHash(message.fileHash)
                    if (existing != null) {
                        manager.sendMessage(endpointId, SyncMessage.AlreadyHave(message.fileHash))
                        _syncFilePath.value = existing.filePath
                    } else {
                        manager.sendMessage(endpointId, SyncMessage.NeedFile(message.fileHash))
                    }
                }
                is SyncMessage.NeedFile -> {
                    val doc = app.database.pdfDocumentDao().getByHash(message.fileHash)
                    if (doc != null) {
                        manager.sendFile(endpointId, doc.filePath)
                    }
                }
                is SyncMessage.AlreadyHave -> {
                    // Follower already has the file, nothing to do
                }
                is SyncMessage.PageChange -> {
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
            } else {
                _syncFilePath.value = tempPath
            }
        }
    }

    /** Called when a FILE payload is received (large PDF). */
    fun handleIncomingFilePath(path: String) {
        viewModelScope.launch {
            val autoSave = app.userPreferences.autoSaveSync
                .stateIn(viewModelScope, SharingStarted.Eagerly, false).value

            if (autoSave) {
                val finalPath = app.fileManager.moveTempToCatalog(path)
                _syncFilePath.value = finalPath
            } else {
                _syncFilePath.value = path
            }
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
        _selectedDocument.value = document
        _role.value = SyncRole.PILOT
        val deviceName = app.userPreferences.deviceName
            .stateIn(viewModelScope, SharingStarted.Eagerly, android.os.Build.MODEL).value
        viewModelScope.launch {
            _sessionManager.value?.startAdvertising(deviceName)
        }
    }

    fun startAsFollower() {
        _role.value = SyncRole.FOLLOWER
        viewModelScope.launch {
            _sessionManager.value?.startDiscovery()
        }
    }

    fun connectToEndpoint(endpointId: String) {
        val deviceName = app.userPreferences.deviceName
            .stateIn(viewModelScope, SharingStarted.Eagerly, android.os.Build.MODEL).value
        _sessionManager.value?.requestConnection(endpointId, deviceName)
    }

    fun broadcastPageChange(page: Int) {
        _sessionManager.value?.broadcastMessage(SyncMessage.PageChange(page))
    }

    fun announcePilotSession() {
        val doc = _selectedDocument.value ?: return
        viewModelScope.launch {
            val message = SyncMessage.SessionInfo(
                title = doc.title,
                pageCount = doc.pageCount,
                fileHash = doc.fileHash,
            )
            _sessionManager.value?.broadcastMessage(message)
        }
    }

    fun stopSession() {
        _sessionManager.value?.run {
            stopAdvertising()
            stopDiscovery()
            disconnectAll()
        }
        _role.value = SyncRole.NONE
        _selectedDocument.value = null
        _syncFilePath.value = null
        _syncPage.value = null
        _isDetached.value = false
        _pilotCurrentPage.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }
}
