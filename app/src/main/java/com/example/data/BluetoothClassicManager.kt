package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.model.BluetoothConnectionState
import com.example.model.BluetoothDeviceUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

/**
 * Manages Bluetooth Classic SPP (Serial Port Profile) connections to an ESP32-WROOM-32.
 */
class BluetoothClassicManager(context: Context) {

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val appContext = context.applicationContext
    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var readJob: Job? = null
    private var activeAddress: String? = null
    private var receiversRegistered = false

    private val _connectionState =
        MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Disconnected)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private val _bondedDevices = MutableStateFlow<List<BluetoothDeviceUi>>(emptyList())
    val bondedDevices: StateFlow<List<BluetoothDeviceUi>> = _bondedDevices.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceUi>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceUi>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isBluetoothSupported = MutableStateFlow(bluetoothAdapter != null)
    val isBluetoothSupported: StateFlow<Boolean> = _isBluetoothSupported.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _lastKnownRssi = MutableStateFlow<Int?>(null)
    val lastKnownRssi: StateFlow<Int?> = _lastKnownRssi.asStateFlow()

    private val _incomingLines = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingLines: SharedFlow<String> = _incomingLines.asSharedFlow()

    private val _systemEvents = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val systemEvents: SharedFlow<String> = _systemEvents.asSharedFlow()

    private val btReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    if (!hasConnectPermission()) return
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    val rssiShort = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                    val rssi = if (rssiShort != Short.MIN_VALUE) rssiShort.toInt() else null

                    device?.let { btDevice ->
                        val addr = btDevice.address ?: return
                        if (addr == activeAddress && rssi != null) {
                            _lastKnownRssi.value = rssi
                        }
                        val devName = btDevice.name?.takeIf { it.isNotBlank() } ?: "ESP32 / Dispositivo"
                        val uiDevice = BluetoothDeviceUi(
                            name = devName,
                            address = addr,
                            rssi = rssi,
                            isBonded = btDevice.bondState == BluetoothDevice.BOND_BONDED,
                            isLikelyEsp32 = isLikelyEsp32Name(devName)
                        )
                        val updated = _discoveredDevices.value.toMutableList()
                        val idx = updated.indexOfFirst { it.address == addr }
                        if (idx >= 0) {
                            updated[idx] = uiDevice
                        } else {
                            updated.add(uiDevice)
                        }
                        _discoveredDevices.value = updated
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                    _systemEvents.tryEmit("Escaneo Bluetooth Classic finalizado")
                }

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val enabled = state == BluetoothAdapter.STATE_ON
                    _isBluetoothEnabled.value = enabled
                    if (!enabled) {
                        _isScanning.value = false
                        scope.launch { closeSocketInternal("Bluetooth desactivado en el dispositivo") }
                    } else {
                        refreshBondedDevices()
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    if (device?.address != null && device.address == activeAddress) {
                        scope.launch {
                            closeSocketInternal("Enlace ACL perdido con ${device.address}")
                        }
                    }
                }
            }
        }
    }

    fun registerReceivers() {
        if (receiversRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(btReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            appContext.registerReceiver(btReceiver, filter)
        }
        receiversRegistered = true
        refreshAdapterState()
    }

    fun unregisterReceivers() {
        if (!receiversRegistered) return
        try {
            appContext.unregisterReceiver(btReceiver)
        } catch (_: IllegalArgumentException) {
        }
        receiversRegistered = false
    }

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return requiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(appContext, permission) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun refreshAdapterState() {
        _isBluetoothSupported.value = bluetoothAdapter != null
        _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
        if (_isBluetoothEnabled.value && hasConnectPermission()) {
            refreshBondedDevices()
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshBondedDevices() {
        val adapter = bluetoothAdapter ?: return
        if (!hasConnectPermission() || !adapter.isEnabled) {
            _bondedDevices.value = emptyList()
            return
        }
        val list = try {
            adapter.bondedDevices?.map { device ->
                val devName = device.name?.takeIf { it.isNotBlank() } ?: "Dispositivo BT"
                val knownRssi = _discoveredDevices.value.firstOrNull { it.address == device.address }?.rssi
                BluetoothDeviceUi(
                    name = devName,
                    address = device.address,
                    rssi = knownRssi,
                    isBonded = true,
                    isLikelyEsp32 = isLikelyEsp32Name(devName)
                )
            }?.sortedByDescending { it.isLikelyEsp32 } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
        _bondedDevices.value = list
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _systemEvents.tryEmit("Adaptador Bluetooth no disponible en este dispositivo")
            return
        }
        if (!hasScanPermission() || !hasConnectPermission()) {
            _systemEvents.tryEmit("Permisos Bluetooth requeridos para escanear")
            return
        }
        if (!adapter.isEnabled) {
            _systemEvents.tryEmit("Activa Bluetooth antes de escanear")
            return
        }
        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            _discoveredDevices.value = emptyList()
            val started = adapter.startDiscovery()
            _isScanning.value = started
            if (started) {
                _systemEvents.tryEmit("Buscando módulos ESP32 / Bluetooth Classic cercanos…")
            } else {
                _systemEvents.tryEmit("No se pudo iniciar el escaneo Bluetooth")
            }
        } catch (e: SecurityException) {
            _isScanning.value = false
            _systemEvents.tryEmit("Permiso denegado al escanear: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        val adapter = bluetoothAdapter ?: return
        if (!hasScanPermission()) return
        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
        } catch (_: SecurityException) {
        }
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceUi: BluetoothDeviceUi) {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _connectionState.value = BluetoothConnectionState.Error("Bluetooth no disponible")
            return
        }
        if (!hasConnectPermission()) {
            _connectionState.value = BluetoothConnectionState.Error("Falta permiso BLUETOOTH_CONNECT")
            return
        }
        if (!adapter.isEnabled) {
            _connectionState.value = BluetoothConnectionState.Error("Bluetooth desactivado")
            return
        }

        scope.launch {
            closeSocketInternal(silent = true)
            stopDiscovery()

            activeAddress = deviceUi.address
            deviceUi.rssi?.let { _lastKnownRssi.value = it }
            _connectionState.value = BluetoothConnectionState.Connecting(deviceUi.name, deviceUi.address)
            _systemEvents.tryEmit("Conectando vía SPP a ${deviceUi.name} (${deviceUi.address})…")

            try {
                val device = adapter.getRemoteDevice(deviceUi.address)
                var btSocket: BluetoothSocket? = null
                try {
                    btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    btSocket.connect()
                } catch (primaryEx: IOException) {
                    try {
                        btSocket?.close()
                    } catch (_: IOException) {
                    }
                    // Fallback RFCOMM channel 1 for ESP32 / HC-05 variants
                    val fallbackMethod = device.javaClass.getMethod(
                        "createRfcommSocket",
                        Int::class.javaPrimitiveType
                    )
                    btSocket = fallbackMethod.invoke(device, 1) as BluetoothSocket
                    btSocket.connect()
                }

                socket = btSocket
                outputStream = btSocket.outputStream
                _connectionState.value = BluetoothConnectionState.Connected(
                    deviceName = deviceUi.name,
                    address = deviceUi.address
                )
                _systemEvents.tryEmit("Enlace SPP establecido con ${deviceUi.name}")
                startReadLoop(btSocket)
            } catch (ex: Exception) {
                activeAddress = null
                _connectionState.value = BluetoothConnectionState.Error(
                    "Error al conectar: ${ex.localizedMessage ?: "sin respuesta del ESP32"}"
                )
                _systemEvents.tryEmit("Fallo de conexión SPP: ${ex.localizedMessage}")
            }
        }
    }

    private fun startReadLoop(btSocket: BluetoothSocket) {
        readJob?.cancel()
        readJob = scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(btSocket.inputStream, Charsets.UTF_8))
                while (isActive && btSocket.isConnected) {
                    val line = reader.readLine() ?: break
                    if (line.isNotBlank()) {
                        _incomingLines.tryEmit(line)
                    }
                }
                closeSocketInternal("El ESP32 cerró la conexión Bluetooth")
            } catch (ex: IOException) {
                if (isActive) {
                    closeSocketInternal("Conexión interrumpida: ${ex.localizedMessage ?: "desconectado"}")
                }
            }
        }
    }

    suspend fun sendLine(command: String): Boolean = withContext(Dispatchers.IO) {
        val stream = outputStream ?: return@withContext false
        writeMutex.withLock {
            try {
                val payload = if (command.endsWith("\n")) command else "$command\n"
                stream.write(payload.toByteArray(Charsets.UTF_8))
                stream.flush()
                true
            } catch (ex: IOException) {
                closeSocketInternal("Error de transmisión TX: ${ex.localizedMessage}")
                false
            }
        }
    }

    fun disconnect(stopCommand: String? = null) {
        scope.launch {
            if (stopCommand != null && outputStream != null) {
                try {
                    sendLine(stopCommand)
                } catch (_: Exception) {
                }
            }
            closeSocketInternal("Desconectado por el operador")
        }
    }

    private suspend fun closeSocketInternal(reason: String? = null, silent: Boolean = false) {
        readJob?.cancel()
        readJob = null
        writeMutex.withLock {
            try {
                outputStream?.close()
            } catch (_: IOException) {
            }
            try {
                socket?.close()
            } catch (_: IOException) {
            }
            outputStream = null
            socket = null
        }
        activeAddress = null
        if (!silent) {
            _connectionState.value = BluetoothConnectionState.Disconnected
            if (reason != null) {
                _systemEvents.tryEmit(reason)
            }
        }
    }

    private fun isLikelyEsp32Name(name: String): Boolean {
        val upper = name.uppercase()
        return upper.contains("ESP32") ||
            upper.contains("ROVER") ||
            upper.contains("CAR") ||
            upper.contains("BOT") ||
            upper.contains("HC-05") ||
            upper.contains("HC-06") ||
            upper.contains("SPP")
    }
}
