package com.cmfwatch.companion.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.cmfwatch.companion.domain.models.DeviceConnectionState
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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import java.util.UUID

@SuppressLint("MissingPermission")
class CmfBleManager(
    private val context: Context,
    private val authKeyHex: String? = null
) {
    companion object {
        private const val TAG = "CmfBleManager"

        val CMF_SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        val CMF_CMD_CHAR_UUID: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
        val CCCD_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private const val TARGET_DEVICE_NAME_PREFIX = "CMF Watch"
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var cmdCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow(DeviceConnectionState.DISCONNECTED)
    val connectionState: StateFlow<DeviceConnectionState> = _connectionState.asStateFlow()

    private val _batteryState = MutableStateFlow<BatteryState?>(null)
    val batteryState: StateFlow<BatteryState?> = _batteryState.asStateFlow()

    private val _heartRateFlow = MutableSharedFlow<HeartRateSample>(extraBufferCapacity = 64)
    val heartRateFlow: SharedFlow<HeartRateSample> = _heartRateFlow.asSharedFlow()

    private val _stepFlow = MutableSharedFlow<StepInterval>(extraBufferCapacity = 64)
    val stepFlow: SharedFlow<StepInterval> = _stepFlow.asSharedFlow()

    private var sequenceNumber = 0
    private var authKey: ByteArray? = authKeyHex?.let { parseHexKey(it) }
    private var sessionKey: ByteArray? = null
    private var clientNonce: ByteArray = ByteArray(16)
    private var watchNonce: ByteArray? = null

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun parseHexKey(hex: String): ByteArray {
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    fun connect(macAddress: String) {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG, "Bluetooth disabled or unavailable.")
            _connectionState.value = DeviceConnectionState.ERROR
            return
        }

        val device = bluetoothAdapter!!.getRemoteDevice(macAddress)
        Log.i(TAG, "Initiating GATT connection to ${device.address}...")
        _connectionState.value = DeviceConnectionState.CONNECTING

        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = DeviceConnectionState.DISCONNECTED
        Log.i(TAG, "GATT client disconnected and closed.")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "GATT connected. Requesting MTU 512...")
                gatt.requestMtu(512)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "GATT disconnected. Status: $status")
                _connectionState.value = DeviceConnectionState.DISCONNECTED
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.i(TAG, "MTU configured to $mtu. Discovering services...")
            gatt.discoverServices()
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
            if (cmdCharacteristic != null) {
                Log.i(TAG, "Subscribing to Command notifications ($CMF_CMD_CHAR_UUID)...")
                gatt.setCharacteristicNotification(cmdCharacteristic, true)
                val descriptor = cmdCharacteristic!!.getDescriptor(CCCD_DESCRIPTOR_UUID)
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "CCCD descriptor subscribed. Starting session handshake...")
                startSessionHandshake()
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            handleNotificationBytes(characteristic.value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            handleNotificationBytes(value)
        }
    }

    private fun startSessionHandshake() {
        managerScope.launch {
            try {
                // Step 1: AUTH_PHONE_NAME (0xFFFF 804B)
                val phoneNameBytes = "CMF Companion".toByteArray(Charsets.UTF_8)
                sendFrame(0xFFFF, 0x804B, phoneNameBytes, useEncryption = false)
                delay(200)

                // Step 2: AUTH_NONCE_REQUEST (0xFFFF 804C)
                SecureRandom().nextBytes(clientNonce)
                sendFrame(0xFFFF, 0x804C, clientNonce, useEncryption = false)
            } catch (e: Exception) {
                Log.e(TAG, "Error in session handshake: ${e.message}")
            }
        }
    }

    private fun handleNotificationBytes(data: ByteArray) {
        val frame = ProtocolFramer.parseFrame(data, sessionKey, clientNonce) ?: return

        when {
            // AUTH_NONCE_REPLY (0xFFFF 0003)
            frame.cmd1 == 0xFFFF && frame.cmd2 == 0x0003 -> {
                if (frame.payload.size >= 16 && authKey != null) {
                    watchNonce = ByteArray(16)
                    System.arraycopy(frame.payload, 0, watchNonce!!, 0, 16)
                    sessionKey = CryptoEngine.deriveSessionKey(clientNonce, watchNonce!!, authKey!!)
                    // REDACT SENSITIVE LOGS
                    Log.i(TAG, "Session key established: [REDACTED]")

                    // Confirm authentication (0xFFFF 804D)
                    val confirmBytes = ByteArray(16)
                    sendFrame(0xFFFF, 0x804D, confirmBytes, useEncryption = true)
                    _connectionState.value = DeviceConnectionState.CONNECTED_PAIRED

                    // Immediately fetch battery & battery state
                    fetchBattery()
                }
            }

            // BATTERY_REPLY (0x005C 0x0001)
            frame.cmd1 == 0x005C && frame.cmd2 == 0x0001 -> {
                val battery = TelemetryDecoders.decodeBattery(frame.payload)
                if (battery != null) {
                    _batteryState.value = battery
                    Log.i(TAG, "Decoded Watch Battery: ${battery.level}% (Charging: ${battery.isCharging})")
                }
            }

            // HEART_RATE (0x0053, 0x00DA, 0x00E0)
            frame.cmd1 == 0x0053 || frame.cmd1 == 0x00DA || frame.cmd1 == 0x00E0 -> {
                val hr = TelemetryDecoders.decodeHeartRate(frame.payload, "0x%04X".format(frame.cmd1))
                if (hr != null) {
                    managerScope.launch { _heartRateFlow.emit(hr) }
                    Log.i(TAG, "Decoded Heart Rate sample: ${hr.bpm} BPM at ${hr.timestamp}")
                }
            }

            // ACTIVITY_DATA (0x0056)
            frame.cmd1 == 0x0056 -> {
                val interval = TelemetryDecoders.decodeStepInterval(frame.payload)
                if (interval != null) {
                    managerScope.launch { _stepFlow.emit(interval) }
                }
            }
        }
    }

    fun fetchBattery() {
        sendFrame(0x005C, 0x0002, ByteArray(0), useEncryption = true)
    }

    fun triggerSync() {
        managerScope.launch {
            _connectionState.value = DeviceConnectionState.SYNCING
            sendFrame(0xFFFF, 0x8005, ByteArray(0), useEncryption = true) // ACTIVITY_FETCH_1
            delay(100)
            sendFrame(0xFFFF, 0x9057, ByteArray(0), useEncryption = true) // ACTIVITY_FETCH_2
            delay(3000)
            _connectionState.value = DeviceConnectionState.CONNECTED_PAIRED
        }
    }

    private fun sendFrame(cmd1: Int, cmd2: Int, payload: ByteArray, useEncryption: Boolean) {
        val gatt = bluetoothGatt ?: return
        val char = cmdCharacteristic ?: return

        val key = if (useEncryption) sessionKey else null
        val iv = if (useEncryption) clientNonce else null

        val frameBytes = ProtocolFramer.buildFrame(
            seq = sequenceNumber++,
            cmd1 = cmd1,
            cmd2 = cmd2,
            payload = payload,
            sessionKey = key,
            iv = iv
        )

        char.value = frameBytes
        gatt.writeCharacteristic(char)
    }
}
