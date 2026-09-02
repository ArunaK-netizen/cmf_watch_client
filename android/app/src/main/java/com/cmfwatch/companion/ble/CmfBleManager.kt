package com.cmfwatch.companion.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.domain.models.HeartRateSample
import com.cmfwatch.companion.domain.models.StepInterval
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.ZoneId
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

    private val frameAssembler = FrameAssembler()
    private var authKey: ByteArray? = authKeyHex?.let { parseHexKey(it) }
    private var sessionKey: ByteArray? = authKey

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
                _connectionState.value = DeviceConnectionState.CONNECTED
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
                _connectionState.value = DeviceConnectionState.AUTHENTICATING
                // Step 1: AUTH_PHONE_NAME (cmd1=0xFFFF, cmd2=0x8049)
                val phoneNameBytes = byteArrayOf(0xA5.toByte()) + "CMF Companion".toByteArray(Charsets.UTF_8)
                sendFrame(0xFFFF, 0x8049, phoneNameBytes, useEncryption = false)
                delay(200)

                // Step 2: AUTH_NONCE_REQUEST (cmd1=0xFFFF, cmd2=0x804B)
                sendFrame(0xFFFF, 0x804B, byteArrayOf(0xA5.toByte()), useEncryption = false)
            } catch (e: Exception) {
                Log.e(TAG, "Error in session handshake: ${e.message}")
            }
        }
    }

    private fun handleNotificationBytes(data: ByteArray) {
        val result = frameAssembler.processRawNotification(data, sessionKey) ?: return
        val (opcode, payload) = result
        val (cmd1, cmd2) = opcode

        when {
            // AUTH_NONCE_REPLY (0xFFFF 004C)
            cmd1 == 0xFFFF && cmd2 == 0x004C -> {
                if (payload.size >= 16 && authKey != null) {
                    val watchNonce = payload.copyOfRange(0, 16)
                    sessionKey = CryptoEngine.deriveSessionKey(watchNonce, authKey!!)
                    // REDACT SENSITIVE LOGS
                    Log.i(TAG, "Session key established: [REDACTED]")

                    // Step 4: AUTHENTICATED_CONFIRM_REQUEST (0xFFFF 804D)
                    sendFrame(0xFFFF, 0x804D, byteArrayOf(0xA5.toByte()), useEncryption = true)
                    _connectionState.value = DeviceConnectionState.CONNECTED_PAIRED

                    // Post-Auth Mandatory Command: Set Time (0xFFFF 8004)
                    setDeviceTime()

                    // Immediately query Battery
                    fetchBattery()
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

            // ACTIVITY_DATA (0x0056)
            cmd1 == 0x0056 -> {
                val interval = TelemetryDecoders.decodeStepInterval(payload)
                if (interval != null) {
                    managerScope.launch { _stepFlow.emit(interval) }
                }
            }
        }
    }

    private fun setDeviceTime() {
        managerScope.launch {
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
    }

    fun fetchBattery() {
        sendFrame(0x005C, 0x0002, byteArrayOf(0xA5.toByte()), useEncryption = true)
    }

    fun triggerSync() {
        managerScope.launch {
            _connectionState.value = DeviceConnectionState.SYNCING
            sendFrame(0xFFFF, 0x8005, byteArrayOf(0xA5.toByte()), useEncryption = true) // ACTIVITY_FETCH_1
            delay(100)
            sendFrame(0xFFFF, 0x9057, byteArrayOf(0xA5.toByte()), useEncryption = true) // ACTIVITY_FETCH_2
            delay(3000)
            _connectionState.value = DeviceConnectionState.CONNECTED_PAIRED
        }
    }

    private fun sendFrame(cmd1: Int, cmd2: Int, payload: ByteArray, useEncryption: Boolean) {
        val gatt = bluetoothGatt ?: return
        val char = cmdCharacteristic ?: return

        val key = if (useEncryption) sessionKey else null

        val frames = ProtocolFramer.buildFrames(
            cmd1 = cmd1,
            cmd2 = cmd2,
            payload = payload,
            sessionKey = key
        )

        for (frameBytes in frames) {
            char.value = frameBytes
            gatt.writeCharacteristic(char)
        }
    }
}
