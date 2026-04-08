package com.mazzika.lyrics.data.nearby

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import android.os.ParcelFileDescriptor
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NearbySessionManager(private val context: Context) {

    companion object {
        private const val TAG = "NearbySessionManager"
        const val SERVICE_ID = "com.mazzika.lyrics.sync"
    }

    data class EndpointInfo(val id: String, val name: String)

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)

    private val _connectedEndpoints = MutableStateFlow<List<EndpointInfo>>(emptyList())
    val connectedEndpoints: StateFlow<List<EndpointInfo>> = _connectedEndpoints.asStateFlow()

    private val _discoveredEndpoints = MutableStateFlow<List<EndpointInfo>>(emptyList())
    val discoveredEndpoints: StateFlow<List<EndpointInfo>> = _discoveredEndpoints.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _incomingMessages = MutableStateFlow<Pair<String, SyncMessage>?>(null)
    val incomingMessages: StateFlow<Pair<String, SyncMessage>?> = _incomingMessages.asStateFlow()

    private val _incomingFile = MutableStateFlow<Pair<String, ByteArray>?>(null)
    val incomingFile: StateFlow<Pair<String, ByteArray>?> = _incomingFile.asStateFlow()

    /** Emits the absolute path of a received FILE payload once transfer completes. */
    private val _incomingFilePath = MutableStateFlow<Pair<String, String>?>(null)
    val incomingFilePath: StateFlow<Pair<String, String>?> = _incomingFilePath.asStateFlow()

    /** Tracks in-flight FILE payloads: payloadId -> (endpointId, payload). */
    private val pendingFilePayloads = mutableMapOf<Long, Pair<String, Payload>>()

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Always auto-accept connections
            Log.d(TAG, "Connection initiated from ${info.endpointName} ($endpointId), auto-accepting")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                val name = _discoveredEndpoints.value
                    .firstOrNull { it.id == endpointId }?.name ?: endpointId
                _connectedEndpoints.update { current ->
                    if (current.none { it.id == endpointId }) {
                        current + EndpointInfo(id = endpointId, name = name)
                    } else {
                        current
                    }
                }
                Log.d(TAG, "Connected to endpoint: $endpointId")
            } else {
                Log.w(TAG, "Connection failed to endpoint: $endpointId (${result.status.statusCode})")
            }
        }

        override fun onDisconnected(endpointId: String) {
            _connectedEndpoints.update { current -> current.filter { it.id != endpointId } }
            Log.d(TAG, "Disconnected from endpoint: $endpointId")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val endpoint = EndpointInfo(id = endpointId, name = info.endpointName)
            _discoveredEndpoints.update { current ->
                if (current.none { it.id == endpointId }) current + endpoint else current
            }
            Log.d(TAG, "Discovered endpoint: $endpointId (${info.endpointName})")
        }

        override fun onEndpointLost(endpointId: String) {
            _discoveredEndpoints.update { current -> current.filter { it.id != endpointId } }
            Log.d(TAG, "Lost endpoint: $endpointId")
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val bytes = payload.asBytes() ?: return
                    val message = SyncMessage.fromBytes(bytes)
                    if (message != null) {
                        _incomingMessages.value = Pair(endpointId, message)
                    } else {
                        // Not a message, treat as file bytes (legacy / small files)
                        _incomingFile.value = Pair(endpointId, bytes)
                    }
                }
                Payload.Type.FILE -> {
                    // Track this payload AND the payload itself so we can grab the file when transfer completes
                    pendingFilePayloads[payload.id] = Pair(endpointId, payload)
                    Log.d(TAG, "Receiving FILE payload ${payload.id} from $endpointId")
                }
                else -> Log.d(TAG, "Received unsupported payload type: ${payload.type}")
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            val payloadId = update.payloadId
            Log.d(TAG, "Payload transfer update: id=$payloadId, status=${update.status}, " +
                "bytes=${update.bytesTransferred}/${update.totalBytes}")

            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                val (senderEndpoint, payload) = pendingFilePayloads.remove(payloadId) ?: return

                // Get the received file from the payload
                val receivedFile = payload.asFile()?.asJavaFile()
                if (receivedFile != null && receivedFile.exists()) {
                    val destFile = File(context.filesDir, "temp/${System.currentTimeMillis()}_sync_received.pdf")
                    destFile.parentFile?.mkdirs()
                    receivedFile.renameTo(destFile)
                    _incomingFilePath.value = Pair(senderEndpoint, destFile.absolutePath)
                    Log.d(TAG, "FILE payload $payloadId completed: ${destFile.absolutePath}")
                } else {
                    // Fallback: try the legacy path
                    val legacyFile = File(context.filesDir, payloadId.toString())
                    Log.d(TAG, "Trying legacy path: ${legacyFile.absolutePath}, exists=${legacyFile.exists()}")
                    if (legacyFile.exists()) {
                        val destFile = File(context.filesDir, "temp/${System.currentTimeMillis()}_sync_received.pdf")
                        destFile.parentFile?.mkdirs()
                        legacyFile.renameTo(destFile)
                        _incomingFilePath.value = Pair(senderEndpoint, destFile.absolutePath)
                        Log.d(TAG, "FILE payload $payloadId completed (legacy): ${destFile.absolutePath}")
                    } else {
                        Log.e(TAG, "FILE payload $payloadId completed but file not found! " +
                            "receivedFile=${receivedFile?.absolutePath}, legacyPath=${legacyFile.absolutePath}")
                    }
                }
            } else if (update.status == PayloadTransferUpdate.Status.FAILURE) {
                pendingFilePayloads.remove(payloadId)
                Log.e(TAG, "FILE payload $payloadId FAILED")
            }
        }
    }

    fun startAdvertising(deviceName: String) {
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
        connectionsClient.startAdvertising(deviceName, SERVICE_ID, connectionLifecycleCallback, options)
            .addOnSuccessListener { _isAdvertising.value = true }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to start advertising", e)
            }
    }

    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        _isAdvertising.value = false
    }

    fun startDiscovery() {
        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener { _isDiscovering.value = true }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to start discovery", e)
            }
    }

    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        _isDiscovering.value = false
        _discoveredEndpoints.value = emptyList()
    }

    fun requestConnection(endpointId: String, localDeviceName: String) {
        connectionsClient.requestConnection(localDeviceName, endpointId, connectionLifecycleCallback)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to request connection to $endpointId", e)
            }
    }

    fun acceptConnection(endpointId: String) {
        connectionsClient.acceptConnection(endpointId, payloadCallback)
    }

    fun sendMessage(endpointId: String, message: SyncMessage) {
        val payload = Payload.fromBytes(message.toBytes())
        connectionsClient.sendPayload(endpointId, payload)
    }

    fun broadcastMessage(message: SyncMessage) {
        val payload = Payload.fromBytes(message.toBytes())
        val endpointIds = _connectedEndpoints.value.map { it.id }
        if (endpointIds.isNotEmpty()) {
            connectionsClient.sendPayload(endpointIds, payload)
        }
    }

    fun sendFile(endpointId: String, filePath: String) {
        try {
            val file = File(filePath)
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val payload = Payload.fromFile(pfd)
            connectionsClient.sendPayload(endpointId, payload)
            Log.d(TAG, "Sending FILE payload to $endpointId (${file.length()} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send file to $endpointId", e)
        }
    }

    fun disconnect(endpointId: String) {
        connectionsClient.disconnectFromEndpoint(endpointId)
        _connectedEndpoints.update { current -> current.filter { it.id != endpointId } }
    }

    fun disconnectAll() {
        connectionsClient.stopAllEndpoints()
        _connectedEndpoints.value = emptyList()
        _discoveredEndpoints.value = emptyList()
        _isAdvertising.value = false
        _isDiscovering.value = false
    }
}
