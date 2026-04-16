package com.mazzika.lyrics.data.nearby

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mazzika.lyrics.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class NearbyService : Service() {

    companion object {
        private const val CHANNEL_ID = "mazzika_sync"
        private const val NOTIFICATION_ID = 1001
    }

    inner class LocalBinder : Binder() {
        fun getSessionManager(): NearbySessionManager = sessionManager
        fun getService(): NearbyService = this@NearbyService
        /** Proactively clear the foreground notification — safe to call from callers
         *  that are about to unbind/stop the service. */
        fun tearDownForeground() = clearForegroundNotification()
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var sessionManager: NearbySessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        sessionManager = NearbySessionManager(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(0))

        // Update notification when connected endpoints change
        sessionManager.connectedEndpoints.onEach { endpoints ->
            updateNotification(endpoints.size)
        }.launchIn(serviceScope)
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Session is one-shot — we don't want the system to resurrect the service
        // after we explicitly stop it.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        clearForegroundNotification()
        sessionManager.disconnectAll()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun clearForegroundNotification() {
        // Tear down the foreground notification. STOP_FOREGROUND_REMOVE guarantees
        // the notification is removed (not just detached).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        // Extra safety: explicitly cancel the notification in case the platform
        // kept a stale copy (seen on some OEMs).
        getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Session Mazzika",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Synchronisation active entre appareils"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(connectedCount: Int) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Session Mazzika active — $connectedCount appareils connectés")
        .setContentText("Synchronisation en cours")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    private fun updateNotification(connectedCount: Int) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(connectedCount))
    }
}
