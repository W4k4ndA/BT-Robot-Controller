package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BluetoothClassicManager
import com.example.data.HostBatteryMonitor
import com.example.domain.DifferentialDriveCalculator
import com.example.domain.Esp32TelemetryParser
import com.example.model.BluetoothConnectionState
import com.example.model.BluetoothDeviceUi
import com.example.model.ConsoleLogEntry
import com.example.model.Esp32TelemetryState
import com.example.model.HostBatteryState
import com.example.model.LogCategory
import com.example.model.MotorCommandFormat
import com.example.model.MotorOutput
import com.example.model.RoverSettings
import com.example.model.RoverTab
import com.example.model.SpeedPreset
import com.example.model.TelemetrySample
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

data class RoverUiState(
    val selectedTab: RoverTab = RoverTab.COCKPIT,
    val connectionState: BluetoothConnectionState = BluetoothConnectionState.Disconnected,
    val isBluetoothSupported: Boolean = true,
    val isBluetoothEnabled: Boolean = false,
    val hasBluetoothPermissions: Boolean = false,
    val isScanning: Boolean = false,
    val bondedDevices: List<BluetoothDeviceUi> = emptyList(),
    val discoveredDevices: List<BluetoothDeviceUi> = emptyList(),
    val joystickX: Float = 0f,
    val joystickY: Float = 0f,
    val motorOutput: MotorOutput = MotorOutput(0, 0, 0f, 0f, 0f, "DETENIDO"),
    val lastSentCommand: String = "L:0,R:0",
    val isEmergencyStopped: Boolean = false,
    val settings: RoverSettings = RoverSettings(),
    val hostBattery: HostBatteryState = HostBatteryState(),
    val esp32Telemetry: Esp32TelemetryState = Esp32TelemetryState(),
    val discoveryRssiDbm: Int? = null,
    val telemetryHistory: List<TelemetrySample> = emptyList(),
    val consoleLogs: List<ConsoleLogEntry> = emptyList(),
    val selectedLogFilter: LogCategory? = null,
    val isConsolePaused: Boolean = false,
    val isBluetoothSheetOpen: Boolean = false,
    val isSettingsSheetOpen: Boolean = false
)

class RoverViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager = BluetoothClassicManager(application)
    private val hostBatteryMonitor = HostBatteryMonitor(application)

    private val _uiState = MutableStateFlow(
        RoverUiState(hasBluetoothPermissions = bluetoothManager.hasRequiredPermissions())
    )
    val uiState: StateFlow<RoverUiState> = _uiState.asStateFlow()

    private val logIdGen = AtomicLong(1L)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private var lastTransmittedCommand: String = ""
    private var lastTransmitTimeMs: Long = 0L

    init {
        bluetoothManager.registerReceivers()
        hostBatteryMonitor.start()

        appendLog(LogCategory.SYS, "Cabina RoverLink inicializada • Modo Bluetooth Classic SPP")

        observeBluetoothFlows()
        observeHostBattery()
        startMotorCommandLoop()
        startTelemetryWaveformSampler()
    }

    private fun observeBluetoothFlows() {
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { connState ->
                _uiState.update { it.copy(connectionState = connState) }
                if (connState is BluetoothConnectionState.Disconnected ||
                    connState is BluetoothConnectionState.Error
                ) {
                    lastTransmittedCommand = ""
                }
            }
        }
        viewModelScope.launch {
            bluetoothManager.isBluetoothSupported.collect { supported ->
                _uiState.update { it.copy(isBluetoothSupported = supported) }
            }
        }
        viewModelScope.launch {
            bluetoothManager.isBluetoothEnabled.collect { enabled ->
                _uiState.update { it.copy(isBluetoothEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            bluetoothManager.isScanning.collect { scanning ->
                _uiState.update { it.copy(isScanning = scanning) }
            }
        }
        viewModelScope.launch {
            bluetoothManager.bondedDevices.collect { devices ->
                _uiState.update { it.copy(bondedDevices = devices) }
            }
        }
        viewModelScope.launch {
            bluetoothManager.discoveredDevices.collect { devices ->
                _uiState.update { it.copy(discoveredDevices = devices) }
            }
        }
        viewModelScope.launch {
            bluetoothManager.lastKnownRssi.collect { rssi ->
                _uiState.update { it.copy(discoveryRssiDbm = rssi) }
            }
        }
        viewModelScope.launch {
            bluetoothManager.systemEvents.collect { msg ->
                val cat = if (msg.contains("Error", ignoreCase = true) ||
                    msg.contains("Fallo", ignoreCase = true)
                ) {
                    LogCategory.ERR
                } else {
                    LogCategory.SYS
                }
                appendLog(cat, msg)
            }
        }
        viewModelScope.launch {
            bluetoothManager.incomingLines.collect { rawLine ->
                handleIncomingEsp32Line(rawLine)
            }
        }
    }

    private fun observeHostBattery() {
        viewModelScope.launch {
            hostBatteryMonitor.batteryState.collect { battery ->
                _uiState.update { it.copy(hostBattery = battery) }
            }
        }
    }

    /**
     * Streams differential motor command strings at ~20 Hz while connected,
     * immediately pushing changes or zero-stop commands without flooding SPP buffers.
     */
    private fun startMotorCommandLoop() {
        viewModelScope.launch {
            while (isActive) {
                val state = _uiState.value
                if (state.connectionState is BluetoothConnectionState.Connected) {
                    val cmd = DifferentialDriveCalculator.formatCommand(
                        leftSpeed = state.motorOutput.leftSpeed,
                        rightSpeed = state.motorOutput.rightSpeed,
                        format = state.settings.commandFormat
                    )
                    val now = System.currentTimeMillis()
                    val isMoving = state.motorOutput.leftSpeed != 0 || state.motorOutput.rightSpeed != 0
                    val changed = cmd != lastTransmittedCommand
                    val heartbeatDue = isMoving && (now - lastTransmitTimeMs >= 150L)

                    if (changed || heartbeatDue) {
                        val sent = bluetoothManager.sendLine(cmd)
                        if (sent) {
                            lastTransmittedCommand = cmd
                            lastTransmitTimeMs = now
                            _uiState.update {
                                it.copy(
                                    lastSentCommand = cmd,
                                    esp32Telemetry = it.esp32Telemetry.copy(
                                        packetsSent = it.esp32Telemetry.packetsSent + 1
                                    )
                                )
                            }
                            if (changed) {
                                appendLog(LogCategory.TX, cmd)
                            }
                        }
                    }
                }
                delay(50L)
            }
        }
    }

    /**
     * Records 5 Hz motor velocity samples for the real-time diagnostics waveform graph.
     */
    private fun startTelemetryWaveformSampler() {
        viewModelScope.launch {
            while (isActive) {
                val state = _uiState.value
                val sample = TelemetrySample(
                    timestampMs = System.currentTimeMillis(),
                    txLeft = state.motorOutput.leftSpeed,
                    txRight = state.motorOutput.rightSpeed,
                    rxLeft = state.esp32Telemetry.reportedLeftSpeed,
                    rxRight = state.esp32Telemetry.reportedRightSpeed
                )
                _uiState.update { current ->
                    val nextHistory = (current.telemetryHistory + sample).takeLast(50)
                    current.copy(telemetryHistory = nextHistory)
                }
                delay(200L)
            }
        }
    }

    private fun handleIncomingEsp32Line(rawLine: String) {
        val currentTelemetry = _uiState.value.esp32Telemetry
        val parsed = Esp32TelemetryParser.parseLine(rawLine, currentTelemetry)

        _uiState.update { state ->
            state.copy(esp32Telemetry = parsed.updatedState)
        }

        if (parsed.isDebugMessage && parsed.debugText != null) {
            appendLog(LogCategory.DEBUG, parsed.debugText)
        } else {
            appendLog(LogCategory.RX, rawLine.trim())
        }
    }

    fun selectTab(tab: RoverTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun updateJoystick(x: Float, y: Float) {
        val state = _uiState.value
        if (state.isEmergencyStopped && (x != 0f || y != 0f)) {
            _uiState.update { it.copy(isEmergencyStopped = false) }
        }
        val output = DifferentialDriveCalculator.calculateMotors(
            joystickX = x,
            joystickY = y,
            maxPwm = state.settings.maxPwm,
            deadzone = state.settings.deadzone,
            speedScale = state.settings.speedPreset.scale,
            invertLeft = state.settings.invertLeft,
            invertRight = state.settings.invertRight
        )
        val previewCmd = DifferentialDriveCalculator.formatCommand(
            output.leftSpeed,
            output.rightSpeed,
            state.settings.commandFormat
        )
        _uiState.update {
            it.copy(
                joystickX = x,
                joystickY = y,
                motorOutput = output,
                lastSentCommand = previewCmd
            )
        }

        // Send immediate zero-stop packet when releasing stick to center
        if (output.leftSpeed == 0 && output.rightSpeed == 0 &&
            lastTransmittedCommand != previewCmd &&
            state.connectionState is BluetoothConnectionState.Connected
        ) {
            viewModelScope.launch {
                if (bluetoothManager.sendLine(previewCmd)) {
                    lastTransmittedCommand = previewCmd
                    lastTransmitTimeMs = System.currentTimeMillis()
                    appendLog(LogCategory.TX, previewCmd)
                }
            }
        }
    }

    fun triggerEmergencyStop() {
        val zeroCmd = DifferentialDriveCalculator.formatCommand(
            0,
            0,
            _uiState.value.settings.commandFormat
        )
        _uiState.update {
            it.copy(
                joystickX = 0f,
                joystickY = 0f,
                isEmergencyStopped = true,
                motorOutput = MotorOutput(0, 0, 0f, 0f, 0f, "E-STOP"),
                lastSentCommand = zeroCmd
            )
        }
        appendLog(LogCategory.SYS, "PARADA DE EMERGENCIA ACTIVADA -> $zeroCmd")
        if (_uiState.value.connectionState is BluetoothConnectionState.Connected) {
            viewModelScope.launch {
                if (bluetoothManager.sendLine(zeroCmd)) {
                    lastTransmittedCommand = zeroCmd
                    lastTransmitTimeMs = System.currentTimeMillis()
                    appendLog(LogCategory.TX, zeroCmd)
                }
            }
        }
    }

    fun sendManualCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return
        if (_uiState.value.connectionState !is BluetoothConnectionState.Connected) {
            appendLog(LogCategory.ERR, "No hay enlace Bluetooth activo para enviar: $trimmed")
            return
        }
        viewModelScope.launch {
            val sent = bluetoothManager.sendLine(trimmed)
            if (sent) {
                _uiState.update {
                    it.copy(
                        esp32Telemetry = it.esp32Telemetry.copy(
                            packetsSent = it.esp32Telemetry.packetsSent + 1
                        )
                    )
                }
                appendLog(LogCategory.TX, trimmed)
            }
        }
    }

    fun setSpeedPreset(preset: SpeedPreset) {
        _uiState.update { state ->
            val updatedSettings = state.settings.copy(speedPreset = preset)
            val updatedOutput = DifferentialDriveCalculator.calculateMotors(
                joystickX = state.joystickX,
                joystickY = state.joystickY,
                maxPwm = updatedSettings.maxPwm,
                deadzone = updatedSettings.deadzone,
                speedScale = updatedSettings.speedPreset.scale,
                invertLeft = updatedSettings.invertLeft,
                invertRight = updatedSettings.invertRight
            )
            state.copy(
                settings = updatedSettings,
                motorOutput = updatedOutput,
                lastSentCommand = DifferentialDriveCalculator.formatCommand(
                    updatedOutput.leftSpeed,
                    updatedOutput.rightSpeed,
                    updatedSettings.commandFormat
                )
            )
        }
    }

    fun setCommandFormat(format: MotorCommandFormat) {
        _uiState.update { state ->
            val updatedSettings = state.settings.copy(commandFormat = format)
            state.copy(
                settings = updatedSettings,
                lastSentCommand = DifferentialDriveCalculator.formatCommand(
                    state.motorOutput.leftSpeed,
                    state.motorOutput.rightSpeed,
                    format
                )
            )
        }
        appendLog(LogCategory.SYS, "Formato TX actualizado: ${format.displayName} (${format.example})")
    }

    fun setMaxPwm(maxPwm: Int) {
        _uiState.update { state ->
            val updatedSettings = state.settings.copy(maxPwm = maxPwm)
            val updatedOutput = DifferentialDriveCalculator.calculateMotors(
                joystickX = state.joystickX,
                joystickY = state.joystickY,
                maxPwm = maxPwm,
                deadzone = updatedSettings.deadzone,
                speedScale = updatedSettings.speedPreset.scale,
                invertLeft = updatedSettings.invertLeft,
                invertRight = updatedSettings.invertRight
            )
            state.copy(settings = updatedSettings, motorOutput = updatedOutput)
        }
    }

    fun setInvertLeft(invert: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(invertLeft = invert)) }
    }

    fun setInvertRight(invert: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(invertRight = invert)) }
    }

    fun setDeadzone(deadzone: Float) {
        _uiState.update { it.copy(settings = it.settings.copy(deadzone = deadzone.coerceIn(0f, 0.25f))) }
    }

    fun openBluetoothSheet() {
        refreshBluetoothState()
        _uiState.update { it.copy(isBluetoothSheetOpen = true) }
    }

    fun closeBluetoothSheet() {
        bluetoothManager.stopDiscovery()
        _uiState.update { it.copy(isBluetoothSheetOpen = false) }
    }

    fun openSettingsSheet() {
        _uiState.update { it.copy(isSettingsSheetOpen = true) }
    }

    fun closeSettingsSheet() {
        _uiState.update { it.copy(isSettingsSheetOpen = false) }
    }

    fun requiredBluetoothPermissions(): Array<String> = bluetoothManager.requiredPermissions()

    fun onPermissionsResult() {
        val granted = bluetoothManager.hasRequiredPermissions()
        _uiState.update { it.copy(hasBluetoothPermissions = granted) }
        if (granted) {
            bluetoothManager.refreshAdapterState()
            appendLog(LogCategory.SYS, "Permisos Bluetooth concedidos")
        } else {
            appendLog(LogCategory.ERR, "Permisos Bluetooth denegados por el usuario")
        }
    }

    fun refreshBluetoothState() {
        val granted = bluetoothManager.hasRequiredPermissions()
        _uiState.update { it.copy(hasBluetoothPermissions = granted) }
        bluetoothManager.refreshAdapterState()
    }

    fun startBluetoothScan() {
        bluetoothManager.startDiscovery()
    }

    fun stopBluetoothScan() {
        bluetoothManager.stopDiscovery()
    }

    fun connectToDevice(device: BluetoothDeviceUi) {
        _uiState.update {
            it.copy(
                isBluetoothSheetOpen = false,
                esp32Telemetry = Esp32TelemetryState()
            )
        }
        bluetoothManager.connect(device)
    }

    fun disconnectBluetooth() {
        val zeroCmd = DifferentialDriveCalculator.formatCommand(
            0,
            0,
            _uiState.value.settings.commandFormat
        )
        updateJoystick(0f, 0f)
        bluetoothManager.disconnect(zeroCmd)
    }

    fun setLogFilter(category: LogCategory?) {
        _uiState.update { it.copy(selectedLogFilter = category) }
    }

    fun toggleConsolePaused() {
        _uiState.update { it.copy(isConsolePaused = !it.isConsolePaused) }
    }

    fun clearConsoleLogs() {
        _uiState.update { it.copy(consoleLogs = emptyList()) }
    }

    fun onAppPausedSafetyStop() {
        if (_uiState.value.joystickX != 0f || _uiState.value.joystickY != 0f) {
            updateJoystick(0f, 0f)
        }
    }

    private fun appendLog(category: LogCategory, message: String) {
        if (_uiState.value.isConsolePaused && category != LogCategory.ERR) return
        val entry = ConsoleLogEntry(
            id = logIdGen.getAndIncrement(),
            timestampFormatted = timeFormat.format(Date()),
            category = category,
            message = message
        )
        _uiState.update { state ->
            val updated = (state.consoleLogs + entry).takeLast(250)
            state.copy(consoleLogs = updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        val zeroCmd = DifferentialDriveCalculator.formatCommand(
            0,
            0,
            _uiState.value.settings.commandFormat
        )
        bluetoothManager.disconnect(zeroCmd)
        bluetoothManager.unregisterReceivers()
        hostBatteryMonitor.stop()
    }
}
