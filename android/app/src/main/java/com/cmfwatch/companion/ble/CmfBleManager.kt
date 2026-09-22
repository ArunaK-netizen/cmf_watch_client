package com.cmfwatch.companion.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.domain.models.DiscoveredDevice
import com.cmfwatch.companion.domain.models.HeartRateSample
import com.cmfwatch.companion.domain.models.SpO2Sample
import com.cmfwatch.companion.domain.models.StepInterval
import com.cmfwatch.companion.domain.models.StressSample
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@SuppressLint("MissingPermission")
class CmfBleManager(
    private val context: Context,
    authKeyHex: String? = null
) {
    companion object {
        private const val TAG = "CmfBleManager"

        @Volatile
        private var INSTANCE: CmfBleManager? = null

        fun getInstance(context: Context, authKeyHex: String? = null): CmfBleManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CmfBleManager(context.applicationContext, authKeyHex).also { INSTANCE = it }
            }
        }

        val CMF_SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        val CMF_CMD_CHAR_UUID: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
        val CMF_CMD_WRITE_CHAR_UUID: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
        val SHELL_SERVICE_UUID: UUID = UUID.fromString("77d4e67c-2fe2-2334-0d35-9ccd078f529c")
        val SHELL_WRITE_CHAR_UUID: UUID = UUID.fromString("77d4ff01-2fe2-2334-0d35-9ccd078f529c")
        val SHELL_NOTIFY_CHAR_UUID: UUID = UUID.fromString("77d4ff02-2fe2-2334-0d35-9ccd078f529c")
        val CCCD_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private val appPrefs: SharedPreferences = context.getSharedPreferences("cmf_watch_prefs", Context.MODE_PRIVATE)

    private var bluetoothGatt: BluetoothGatt? = null
    private var cmdCharacteristic: BluetoothGattCharacteristic? = null
    private var cmdWriteCharacteristic: BluetoothGattCharacteristic? = null
    private var shellWriteCharacteristic: BluetoothGattCharacteristic? = null
    private var shellNotifyCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow(DeviceConnectionState.IDLE)
    val connectionState: StateFlow<DeviceConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _batteryState = MutableStateFlow<BatteryState?>(null)
    val batteryState: StateFlow<BatteryState?> = _batteryState.asStateFlow()

    private val _heartRateFlow = MutableSharedFlow<HeartRateSample>(extraBufferCapacity = 64)
    val heartRateFlow: SharedFlow<HeartRateSample> = _heartRateFlow.asSharedFlow()

    private val _stepFlow = MutableSharedFlow<StepInterval>(extraBufferCapacity = 64)
    val stepFlow: SharedFlow<StepInterval> = _stepFlow.asSharedFlow()

    private val _spO2Flow = MutableSharedFlow<SpO2Sample>(extraBufferCapacity = 64)
    val spO2Flow: SharedFlow<SpO2Sample> = _spO2Flow.asSharedFlow()

    private val _stressFlow = MutableSharedFlow<StressSample>(extraBufferCapacity = 64)
    val stressFlow: SharedFlow<StressSample> = _stressFlow.asSharedFlow()

    private val frameAssembler = FrameAssembler()
    private var authKey: ByteArray? = authKeyHex?.let { parseHexKey(it) } ?: loadStoredAuthKey()
    private var sessionKey: ByteArray? = null
    private var pairingSecret: ByteArray? = null
    private var pendingPairRnd1: ByteArray? = null

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gattWriteMutex = Mutex()
    private var isScanning = false
    private var authHandshakeJob: Job? = null
    private var authHandshakeActive = false
    private var subscribeTimeoutJob: Job? = null
    private var pendingGattWrite: CompletableDeferred<Boolean>? = null

    private fun parseHexKey(hex: String): ByteArray {
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun loadStoredAuthKey(): ByteArray? {
        val hex = appPrefs.getString("auth_key_hex", null) ?: return null
        return try {
            parseHexKey(hex)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Stored auth key was invalid; clearing it.")
            appPrefs.edit().remove("auth_key_hex").apply()
            null
        }
    }

    private fun persistAuthKey() {
        val key = authKey ?: return
        val hex = key.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        appPrefs.edit().putString("auth_key_hex", hex).apply()
    }

    fun startScan(timeoutMs: Long = 10000) {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.w(TAG, "Bluetooth is disabled or unavailable. Scan cannot start.")
            isScanning = false
            _connectionState.value = DeviceConnectionState.IDLE
            return
        }

        val hasScanPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.BLUETOOTH_SCAN
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasScanPermission || !hasLocationPermission) {
            Log.w(TAG, "Scan cannot start: missing Bluetooth_SCAN or ACCESS_FINE_LOCATION permission.")
            isScanning = false
            _connectionState.value = DeviceConnectionState.IDLE
            return
        }

        val scanner = bluetoothAdapter!!.bluetoothLeScanner
        if (scanner == null) {
            Log.w(TAG, "BluetoothLeScanner unavailable; BLE scanning is not supported on this device.")
            _connectionState.value = DeviceConnectionState.IDLE
            return
        }

        _discoveredDevices.value = emptyList()
        _connectionState.value = DeviceConnectionState.SCANNING
        isScanning = true

        Log.i(TAG, "Starting dynamic BLE scan for CMF watches...")
        try {
            scanner.startScan(scanCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "BLUETOOTH_SCAN permission missing: ${e.message}")
            isScanning = false
            _connectionState.value = DeviceConnectionState.ERROR
            return
        }

        managerScope.launch {
            delay(timeoutMs)
            if (isScanning) {
                stopScan()
            }
        }
    }

    fun stopScan() {
        if (isScanning && bluetoothAdapter != null && bluetoothAdapter!!.isEnabled) {
            try {
                val scanner = bluetoothAdapter!!.bluetoothLeScanner
                scanner?.stopScan(scanCallback)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException during stopScan: ${e.message}")
            }
            isScanning = false
            Log.i(TAG, "BLE scan stopped. Found ${_discoveredDevices.value.size} devices.")
            if (_discoveredDevices.value.isNotEmpty()) {
                _connectionState.value = DeviceConnectionState.DEVICES_FOUND
            } else if (_connectionState.value == DeviceConnectionState.SCANNING) {
                _connectionState.value = DeviceConnectionState.IDLE
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val advertisedName = result.scanRecord?.deviceName ?: device.name
            val serviceUuids = result.scanRecord?.serviceUuids.orEmpty()

            val matchesWatchName = advertisedName?.contains("CMF Watch", ignoreCase = true) == true ||
                advertisedName?.contains("Watch", ignoreCase = true) == true
            val matchesKnownService = serviceUuids.any { uuid ->
                uuid.uuid == CMF_SERVICE_UUID || uuid.uuid == SHELL_SERVICE_UUID
            }

            if (!matchesWatchName && !matchesKnownService) {
                return
            }

            val displayName = advertisedName ?: "CMF Watch"
            val discovered = DiscoveredDevice(
                name = displayName,
                address = device.address,
                rssi = result.rssi
            )

            val current = _discoveredDevices.value.toMutableList()
            if (current.none { it.address == discovered.address }) {
                current.add(discovered)
                _discoveredDevices.value = current
                _connectionState.value = DeviceConnectionState.DEVICES_FOUND
                Log.i(TAG, "Discovered CMF Watch: ${displayName} (${device.address}, RSSI ${result.rssi})")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE scan failed with errorCode $errorCode; staying in non-error idle state until user retries.")
            isScanning = false
            _connectionState.value = DeviceConnectionState.IDLE
        }
    }

    private var userExplicitDisconnect = false
    private var lastConnectedMac: String? = null

    fun connect(macAddress: String) {
        stopScan()

        // Skip redundant reconnect if already connected/paired to the same device
        if ((_connectionState.value == DeviceConnectionState.CONNECTED_PAIRED ||
                    _connectionState.value == DeviceConnectionState.CONNECTED ||
                    _connectionState.value == DeviceConnectionState.CONNECTING) &&
            bluetoothGatt?.device?.address == macAddress
        ) {
            Log.i(TAG, "Already connected or connecting to $macAddress. Preserving GATT session.")
            return
        }

        userExplicitDisconnect = false
        lastConnectedMac = macAddress

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG, "Bluetooth disabled or unavailable.")
            _connectionState.value = DeviceConnectionState.ERROR
            return
        }

        try {
            val device = bluetoothAdapter!!.getRemoteDevice(macAddress)
            Log.i(TAG, "Initiating LE GATT connection to ${device.address}...")
            _connectionState.value = DeviceConnectionState.CONNECTING

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                bluetoothGatt = device.connectGatt(context, false, gattCallback)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission missing: ${e.message}")
            _connectionState.value = DeviceConnectionState.ERROR
        }
    }

    fun disconnect() {
        userExplicitDisconnect = true
        stopScan()
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException during disconnect: ${e.message}")
        }
        bluetoothGatt = null
        _connectionState.value = DeviceConnectionState.DISCONNECTED
        Log.i(TAG, "GATT client disconnected and closed.")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "GATT connected! Discovering services directly...")
                _connectionState.value = DeviceConnectionState.CONNECTED
                try {
                    gatt.discoverServices()
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException discovering services: ${e.message}")
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "GATT disconnected. Status: $status")
                _connectionState.value = DeviceConnectionState.DISCONNECTED

                // Auto-reconnect if link dropped unexpectedly without explicit user disconnect
                if (!userExplicitDisconnect && lastConnectedMac != null) {
                    val macToReconnect = lastConnectedMac!!
                    managerScope.launch {
                        delay(3000)
                        if (!userExplicitDisconnect && _connectionState.value == DeviceConnectionState.DISCONNECTED) {
                            Log.i(TAG, "Attempting background auto-reconnect to $macToReconnect...")
                            connect(macToReconnect)
                        }
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed with status $status")
                _connectionState.value = DeviceConnectionState.ERROR
                return
            }

            val service = gatt.getService(CMF_SERVICE_UUID)
            if (service == null) {
                Log.e(TAG, "CMF Service $CMF_SERVICE_UUID not found.")
                _connectionState.value = DeviceConnectionState.ERROR
                return
            }

            cmdCharacteristic = service.getCharacteristic(CMF_CMD_CHAR_UUID)
            cmdWriteCharacteristic = service.getCharacteristic(CMF_CMD_WRITE_CHAR_UUID)

            if (cmdCharacteristic != null) {
                Log.i(TAG, "Subscribing to Command notifications ($CMF_CMD_CHAR_UUID)...")
                _connectionState.value = DeviceConnectionState.SUBSCRIBING
                subscribeTimeoutJob?.cancel()
                subscribeTimeoutJob = managerScope.launch {
                    delay(10000)
                    if (_connectionState.value == DeviceConnectionState.SUBSCRIBING) {
                        Log.w(TAG, "Command notification subscription timed out. Giving up and disconnecting.")
                        _connectionState.value = DeviceConnectionState.IDLE
                        disconnect()
                    }
                }

                try {
                    val success = gatt.setCharacteristicNotification(cmdCharacteristic, true)
                    val descriptor = cmdCharacteristic!!.getDescriptor(CCCD_DESCRIPTOR_UUID)
                    if (descriptor == null) {
                        Log.w(TAG, "CCCD descriptor not found for command notify characteristic. Subscription cannot proceed.")
                        _connectionState.value = DeviceConnectionState.IDLE
                        disconnect()
                        return
                    }

                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    val wrote = gatt.writeDescriptor(descriptor)
                    if (!wrote) {
                        Log.w(TAG, "BluetoothGatt.writeDescriptor returned false for command notifications.")
                        _connectionState.value = DeviceConnectionState.IDLE
                        disconnect()
                    }
                    if (!success) {
                        Log.w(TAG, "setCharacteristicNotification returned false for command notifications.")
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException subscribing CCCD: ${e.message}")
                    _connectionState.value = DeviceConnectionState.IDLE
                    disconnect()
                }
            }

            if (cmdWriteCharacteristic == null) {
                Log.w(TAG, "Command write characteristic ${CMF_CMD_WRITE_CHAR_UUID} not found.")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "CCCD write failed for ${descriptor.characteristic.uuid} status=$status")
                return
            }

            if (descriptor.characteristic.uuid == cmdCharacteristic?.uuid) {
                subscribeTimeoutJob?.cancel()
                subscribeTimeoutJob = null
                Log.i(TAG, "Command CCCD subscribed. Starting session handshake...")
                startSessionHandshake()
            } else if (descriptor.characteristic.uuid == shellNotifyCharacteristic?.uuid) {
                Log.i(TAG, "Shell notify CCCD subscribed. Ready for shell pairing exchange.")
                writeShellCommand("AT GETSECRET\r\n")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            Log.i(TAG, "onCharacteristicChanged (legacy): ${characteristic.value?.size} bytes")
            handleNotificationBytes(characteristic.value)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            Log.i(TAG, "onCharacteristicChanged (API 33+): ${value.size} bytes")
            handleNotificationBytes(value)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.i(TAG, "onCharacteristicWrite status=$status (${characteristic.uuid})")
            pendingGattWrite?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }
    }

    private fun startSessionHandshake() {
        if (authHandshakeActive) {
            return
        }

        authHandshakeActive = true
        authHandshakeJob?.cancel()
        authHandshakeJob = managerScope.launch {
            try {
                if (authKey == null) {
                    beginFirstTimePairing()
                    return@launch
                }

                sessionKey = authKey
                _connectionState.value = DeviceConnectionState.AUTHENTICATING
                Log.i(TAG, "Handshake Step 1: Sending AUTH_PHONE_NAME (0xFFFF 8049)...")
                val phoneNameBytes = byteArrayOf(0xA5.toByte()) + "CMF Companion".toByteArray(Charsets.UTF_8)
                sendFrame(0xFFFF, 0x8049, phoneNameBytes, useEncryption = true)

                delay(15000)
                if (_connectionState.value == DeviceConnectionState.AUTHENTICATING) {
                    Log.w(TAG, "Authentication timed out. Resetting to idle state and disconnecting.")
                    _connectionState.value = DeviceConnectionState.IDLE
                    disconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting session handshake: ${e.message}")
                _connectionState.value = DeviceConnectionState.IDLE
            } finally {
                authHandshakeActive = false
            }
        }
    }

    private fun beginFirstTimePairing() {
        authHandshakeJob = managerScope.launch {
            try {
                val shellService = bluetoothGatt?.getService(SHELL_SERVICE_UUID)
                if (shellService == null) {
                    Log.e(TAG, "Shell pairing service not found. Cannot pair device.")
                    _connectionState.value = DeviceConnectionState.IDLE
                    return@launch
                }

                shellWriteCharacteristic = shellService.getCharacteristic(SHELL_WRITE_CHAR_UUID)
                shellNotifyCharacteristic = shellService.getCharacteristic(SHELL_NOTIFY_CHAR_UUID)

                if (shellWriteCharacteristic == null || shellNotifyCharacteristic == null) {
                    Log.e(TAG, "Shell pairing characteristics not found.")
                    _connectionState.value = DeviceConnectionState.IDLE
                    return@launch
                }

                _connectionState.value = DeviceConnectionState.AUTHENTICATING
                Log.i(TAG, "Starting real first-time pairing flow via shell channel.")

                bluetoothGatt?.setCharacteristicNotification(shellNotifyCharacteristic, true)
                val descriptor = shellNotifyCharacteristic?.getDescriptor(CCCD_DESCRIPTOR_UUID)
                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    bluetoothGatt?.writeDescriptor(descriptor)
                } else {
                    Log.w(TAG, "Shell notification CCCD descriptor not found.")
                    _connectionState.value = DeviceConnectionState.IDLE
                    disconnect()
                    return@launch
                }

                delay(20000)
                if (_connectionState.value == DeviceConnectionState.AUTHENTICATING) {
                    Log.w(TAG, "First-time pairing timed out. Resetting to idle state.")
                    _connectionState.value = DeviceConnectionState.IDLE
                    disconnect()
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException during shell pairing setup: ${e.message}")
                _connectionState.value = DeviceConnectionState.IDLE
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected pairing error: ${e.message}")
                _connectionState.value = DeviceConnectionState.IDLE
            } finally {
                authHandshakeActive = false
            }
        }
    }

    private fun beginFirstTimePairingLegacy() {
        val shellService = bluetoothGatt?.getService(SHELL_SERVICE_UUID)
        if (shellService == null) {
            Log.e(TAG, "Shell pairing service not found. Cannot pair device.")
            _connectionState.value = DeviceConnectionState.ERROR
            return
        }

        shellWriteCharacteristic = shellService.getCharacteristic(SHELL_WRITE_CHAR_UUID)
        shellNotifyCharacteristic = shellService.getCharacteristic(SHELL_NOTIFY_CHAR_UUID)

        if (shellWriteCharacteristic == null || shellNotifyCharacteristic == null) {
            Log.e(TAG, "Shell pairing characteristics not found.")
            _connectionState.value = DeviceConnectionState.ERROR
            return
        }

        _connectionState.value = DeviceConnectionState.AUTHENTICATING
        Log.i(TAG, "Starting real first-time pairing flow via shell channel.")

        managerScope.launch {
            try {
                bluetoothGatt?.setCharacteristicNotification(shellNotifyCharacteristic, true)
                val descriptor = shellNotifyCharacteristic?.getDescriptor(CCCD_DESCRIPTOR_UUID)
                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    bluetoothGatt?.writeDescriptor(descriptor)
                }
                writeShellCommand("AT GETSECRET\r\n")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException during shell pairing setup: ${e.message}")
                _connectionState.value = DeviceConnectionState.ERROR
            }
        }
    }

    private fun writeShellCommand(command: String) {
        val shellChar = shellWriteCharacteristic ?: return
        val payload = command.toByteArray(Charsets.UTF_8)
        try {
            shellChar.value = payload
            bluetoothGatt?.writeCharacteristic(shellChar)
            Log.i(TAG, "Shell command sent: ${command.trim()}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException writing shell command: ${e.message}")
        }
    }

    private fun handleShellNotification(bytes: ByteArray) {
        val text = bytes.decodeToString().trim()
        if (text.isEmpty()) return

        Log.i(TAG, "Shell notification: $text")
        if (text.contains("GETSECRET:")) {
            val secretHex = text.substringAfter("GETSECRET:").substringBefore(",OK").trim()
            pairingSecret = bytesFromHex(secretHex)
            val rnd1 = CryptoEngine.generateRandomBytes(16)
            pendingPairRnd1 = rnd1
            val signed1 = signChallenge(rnd1, pairingSecret!!)
            val pairPayload = rnd1 + signed1
            Log.i(TAG, "Sending AUTH_PAIR_REQUEST to watch via command channel.")
            sendFrame(0xFFFF, 0x8047, pairPayload, useEncryption = false)
            return
        }
    }

    private fun handleNotificationBytes(data: ByteArray) {
        if (data.isNotEmpty() && data.any { it in 0x20..0x7E || it == 0x0A.toByte() || it == 0x0D.toByte() }) {
            val text = data.decodeToString().trim()
            if (text.contains("GETSECRET:") || text.contains("AT ") || text.startsWith("OK") || text.startsWith("ERROR")) {
                handleShellNotification(data)
                return
            }
        }

        val result = frameAssembler.processRawNotification(data, sessionKey)
        if (result == null) {
            Log.i(TAG, "Raw notification chunk received (${data.size} bytes), awaiting further frames...")
            return
        }
        val (opcode, payload) = result
        val (cmd1, cmd2) = opcode
        Log.i(TAG, "Notification Opcode Received: cmd1=0x%04X, cmd2=0x%04X, payloadLen=${payload.size}".format(cmd1, cmd2))

        when {
            cmd1 == 0xFFFF && cmd2 == 0x0048 -> {
                val rnd2 = payload.copyOfRange(0, 16)
                val signed2 = payload.copyOfRange(16, 48)
                val expected = signChallenge(rnd2, pairingSecret ?: return)
                if (!signed2.contentEquals(expected)) {
                    Log.e(TAG, "Pair challenge verification failed.")
                    _connectionState.value = DeviceConnectionState.ERROR
                    return
                }
                authKey = CryptoEngine.deriveAuthKey(pendingPairRnd1 ?: return, rnd2, pairingSecret ?: return)
                persistAuthKey()
                sessionKey = authKey
                Log.i(TAG, "First-time pairing verified. Auth key derived successfully and saved locally.")
                _connectionState.value = DeviceConnectionState.CONNECTED_PAIRED
                startSessionHandshake()
            }

            // Step 1 Response: AUTH_WATCH_MAC (0xFFFF 0049) -> Send Step 2: AUTH_NONCE_REQUEST (0xFFFF 804B)
            cmd1 == 0xFFFF && cmd2 == 0x0049 -> {
                Log.i(TAG, "Received AUTH_WATCH_MAC (0xFFFF 0x0049). Step 2: Sending AUTH_NONCE_REQUEST (0xFFFF 0x804B)...")
                sendFrame(0xFFFF, 0x804B, byteArrayOf(0xA5.toByte()), useEncryption = true)
            }

            // Step 2 Response: AUTH_NONCE_REPLY (0xFFFF 0x004C) -> Derive sessionKey & Send Step 3: AUTHENTICATED_CONFIRM_REQUEST (0xFFFF 0x804D)
            cmd1 == 0xFFFF && cmd2 == 0x004C -> {
                Log.i(TAG, "Received AUTH_NONCE_REPLY (0xFFFF 0x004C). Payload len=${payload.size}")
                val watchNonce = if (payload.size >= 16) payload.copyOfRange(0, 16) else payload
                val currentAuthKey = authKey ?: return
                sessionKey = CryptoEngine.deriveSessionKey(watchNonce, currentAuthKey)
                Log.i(TAG, "Session key established: [REDACTED]")

                Log.i(TAG, "Step 3: Sending AUTHENTICATED_CONFIRM_REQUEST (0xFFFF 0x804D)...")
                sendFrame(0xFFFF, 0x804D, byteArrayOf(0xA5.toByte()), useEncryption = true)
            }

            // Step 3 Response: AUTHENTICATED_CONFIRM_REPLY (0xFFFF 0x0004) -> Authentication Complete!
            cmd1 == 0xFFFF && cmd2 == 0x0004 -> {
                Log.i(TAG, "Received AUTHENTICATED_CONFIRM_REPLY (0xFFFF 0x0004). Authentication COMPLETE!")
                _connectionState.value = DeviceConnectionState.CONNECTED_PAIRED
                        managerScope.launch {
                            setDeviceTime()
                            delay(500)
                            fetchBattery()
                            delay(1500)
                            triggerSync()
                        }
            }

            // BATTERY_REPLY (0x005C 0x0001)
            cmd1 == 0x005C && cmd2 == 0x0001 -> {
                val battery = TelemetryDecoders.decodeBattery(payload)
                if (battery != null) {
                    _batteryState.value = battery
                    Log.i(TAG, "Decoded Watch Battery: ${battery.level}% (Charging: ${battery.isCharging})")
                }
            }

            // HEART_RATE (0x0053, 0x00DA, 0x00E0)
            cmd1 == 0x0053 || cmd1 == 0x00DA || cmd1 == 0x00E0 -> {
                val hr = TelemetryDecoders.decodeHeartRate(payload, "0x%04X".format(cmd1))
                if (hr != null) {
                    managerScope.launch { _heartRateFlow.emit(hr) }
                    Log.i(TAG, "Decoded Heart Rate sample: ${hr.bpm} BPM at ${hr.timestamp}")
                }
            }

            // SPO2 (0x0055 0x0001)
            cmd1 == 0x0055 && cmd2 == 0x0001 -> {
                val spO2 = TelemetryDecoders.decodeSpO2(payload)
                if (spO2 != null) {
                    managerScope.launch { _spO2Flow.emit(spO2) }
                    Log.i(TAG, "Decoded SpO2 sample: ${spO2.percentage}% at ${spO2.timestamp}")
                }
            }

            // STRESS (0x009D 0x0001)
            cmd1 == 0x009D && cmd2 == 0x0001 -> {
                val stress = TelemetryDecoders.decodeStress(payload)
                if (stress != null) {
                    managerScope.launch { _stressFlow.emit(stress) }
                    Log.i(TAG, "Decoded stress sample: ${stress.score} at ${stress.timestamp}")
                }
            }

            // ACTIVITY_DATA (0x0056)
            cmd1 == 0x0056 -> {
                val interval = TelemetryDecoders.decodeStepInterval(payload)
                if (interval != null) {
                    managerScope.launch { _stepFlow.emit(interval) }
                }
            }

            // ACTIVITY_FETCH_1 acknowledgement: send the second fetch only after
            // the watch has finished processing the first request.
            cmd1 == 0xFFFF && cmd2 == 0x0005 -> {
                sendFrame(0xFFFF, 0x9057, byteArrayOf(0xA5.toByte()), useEncryption = true)
            }
        }
    }

    private suspend fun setDeviceTime() {
        val now = Instant.now()
        val epochSec = now.epochSecond
        val zone = ZoneId.systemDefault()
        val offsetMs = zone.rules.getOffset(now).totalSeconds * 1000

        val timePayload = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
        timePayload.putInt(epochSec.toInt())
        timePayload.putInt(offsetMs)

        Log.i(TAG, "Setting watch time (epochSec=$epochSec, offsetMs=$offsetMs)...")
        sendFrame(0xFFFF, 0x8004, timePayload.array(), useEncryption = true)
    }

    fun fetchBattery() {
        sendFrame(0x005C, 0x0002, byteArrayOf(0xA5.toByte()), useEncryption = true)
    }

    fun triggerSync() {
        managerScope.launch {
            _connectionState.value = DeviceConnectionState.SYNCING
            sendFrame(0xFFFF, 0x8005, byteArrayOf(0xA5.toByte()), useEncryption = true) // ACTIVITY_FETCH_1
            delay(3000)
            _connectionState.value = DeviceConnectionState.CONNECTED_PAIRED
        }
    }

    private fun sendFrame(cmd1: Int, cmd2: Int, payload: ByteArray, useEncryption: Boolean) {
        managerScope.launch {
            gattWriteMutex.withLock {
                val gatt = bluetoothGatt ?: return@withLock
                val target = cmdWriteCharacteristic ?: cmdCharacteristic ?: return@withLock

                val key = if (useEncryption) sessionKey else null

                val frames = ProtocolFramer.buildFrames(
                    cmd1 = cmd1,
                    cmd2 = cmd2,
                    payload = payload,
                    sessionKey = key
                )

                for (frameBytes in frames) {
                    try {
                        val completion = CompletableDeferred<Boolean>()
                        pendingGattWrite = completion
                        target.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeCharacteristic(target, frameBytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
                        } else {
                            target.value = frameBytes
                            gatt.writeCharacteristic(target)
                        }
                        Log.i(TAG, "sendFrame writeCharacteristic (cmd1=0x%04X, cmd2=0x%04X, len=${frameBytes.size}, enc=$useEncryption) res=$res".format(cmd1, cmd2, frameBytes.size, useEncryption))
                        val writeResult = if (!res) {
                            false
                        } else {
                            withTimeoutOrNull(5000) { completion.await() } ?: false
                        }
                        pendingGattWrite = null
                        if (!writeResult) {
                            Log.w(TAG, "GATT write did not complete successfully for cmd1=0x%04X cmd2=0x%04X".format(cmd1, cmd2))
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException writing characteristic: ${e.message}")
                    }
                }
            }
        }
    }

    private fun signChallenge(rnd: ByteArray, secret: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(rnd)
        md.update(secret)
        return md.digest()
    }

    private fun bytesFromHex(hex: String): ByteArray {
        val clean = hex.replace(" ", "").removePrefix("0x").trim()
        require(clean.length % 2 == 0) { "Invalid hex length: ${clean.length}" }
        return clean.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
