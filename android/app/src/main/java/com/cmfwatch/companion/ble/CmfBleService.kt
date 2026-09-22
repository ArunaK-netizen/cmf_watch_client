package com.cmfwatch.companion.ble

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cmfwatch.companion.MainActivity
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import kotlinx.coroutines.*

class CmfBleService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var bleManager: CmfBleManager

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "CmfBleService created. Initializing background BLE manager...")
        bleManager = CmfBleManager.getInstance(applicationContext)

        createNotificationChannel()

        // Observe connection state to update notification subtitle dynamically
        serviceScope.launch {
            bleManager.connectionState.collect { state ->
                updateNotification(state)
            }
        }

        // Start 5-minute periodic background sampling loop
        serviceScope.launch {
            while (isActive) {
                delay(300_000) // 5 minutes
                if (bleManager.connectionState.value == DeviceConnectionState.CONNECTED_PAIRED) {
                    Log.i(TAG, "Running 5-minute periodic BLE telemetry sync...")
                    bleManager.fetchBattery()
                    bleManager.triggerSync()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "CmfBleService onStartCommand executed.")
        startForegroundServiceNotification(bleManager.connectionState.value)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "CmfBleService destroyed.")
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CMF Watch Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains persistent Bluetooth connection to CMF Watch Pro 2"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundServiceNotification(state: DeviceConnectionState) {
        val notification = buildNotification(state)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(state: DeviceConnectionState) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: DeviceConnectionState): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val statusText = when (state) {
            DeviceConnectionState.CONNECTED_PAIRED, DeviceConnectionState.CONNECTED -> "Connected • Active"
            DeviceConnectionState.CONNECTING -> "Connecting to CMF Watch..."
            DeviceConnectionState.SYNCING -> "Syncing telemetry data..."
            DeviceConnectionState.SCANNING -> "Searching for CMF Watch..."
            else -> "Disconnected • Tap to open"
        }

        val appIcon = applicationInfo.icon

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CMF Watch")
            .setContentText(statusText)
            .setSmallIcon(appIcon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val TAG = "CmfBleService"
        private const val CHANNEL_ID = "cmf_ble_service_channel"
        private const val NOTIFICATION_ID = 9981

        fun start(context: Context) {
            val intent = Intent(context, CmfBleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CmfBleService::class.java)
            context.stopService(intent)
        }
    }
}
