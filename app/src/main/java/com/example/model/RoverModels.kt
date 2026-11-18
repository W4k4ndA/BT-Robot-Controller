package com.example.model

sealed interface BluetoothConnectionState {
    data object Disconnected : BluetoothConnectionState
    data class Connecting(val deviceName: String, val address: String) : BluetoothConnectionState
    data class Connected(
        val deviceName: String,
        val address: String,
        val connectedAtMs: Long = System.currentTimeMillis()
    ) : BluetoothConnectionState
    data class Error(val message: String) : BluetoothConnectionState
}

data class BluetoothDeviceUi(
    val name: String,
    val address: String,
    val rssi: Int? = null,
    val isBonded: Boolean = false,
    val isLikelyEsp32: Boolean = false
)

enum class MotorCommandFormat(
    val id: String,
    val displayName: String,
    val example: String
) {
    LABELED("labeled", "Etiquetado L/R", "L:180,R:240"),
    CSV("csv", "CSV Compacto", "180,240"),
    MOTOR_TAG("motor_tag", "M1 / M2", "M1:180;M2:240"),
    JSON("json", "JSON ESP32", "{\"L\":180,\"R\":240}")
}

enum class SpeedPreset(val scale: Float, val label: String) {
    ECO(0.50f, "ECO 50%"),
    SPORT(0.75f, "SPORT 75%"),
    TURBO(1.00f, "MAX 100%")
}

data class MotorOutput(
    val leftSpeed: Int,
    val rightSpeed: Int,
    val normalizedX: Float,
    val normalizedY: Float,
    val magnitude: Float,
    val directionLabel: String
)

data class TelemetrySample(
    val timestampMs: Long,
    val txLeft: Int,
    val txRight: Int,
    val rxLeft: Int?,
    val rxRight: Int?
)

data class Esp32TelemetryState(
    val batteryPercent: Int? = null,
    val batteryVoltage: Float? = null,
    val reportedLeftSpeed: Int? = null,
    val reportedRightSpeed: Int? = null,
    val distanceCm: Float? = null,
    val temperatureC: Float? = null,
    val currentAmps: Float? = null,
    val obstacleDetected: Boolean? = null,
    val rssiDbm: Int? = null,
    val uptimeSeconds: Long? = null,
    val customSensors: Map<String, String> = emptyMap(),
    val lastPacketTimeMs: Long? = null,
    val packetsReceived: Long = 0L,
    val packetsSent: Long = 0L
)

data class HostBatteryState(
    val percent: Int = 100,
    val isCharging: Boolean = false,
    val voltageMv: Int = 0,
    val temperatureTenthsC: Int = 0
)

enum class LogCategory(val tag: String) {
    RX("RX"),
    TX("TX"),
    DEBUG("DBG"),
    SYS("SYS"),
    ERR("ERR")
}

data class ConsoleLogEntry(
    val id: Long,
    val timestampFormatted: String,
    val category: LogCategory,
    val message: String
)

data class RoverSettings(
    val commandFormat: MotorCommandFormat = MotorCommandFormat.LABELED,
    val maxPwm: Int = 255,
    val deadzone: Float = 0.08f,
    val speedPreset: SpeedPreset = SpeedPreset.TURBO,
    val invertLeft: Boolean = false,
    val invertRight: Boolean = false
)

enum class RoverTab {
    COCKPIT,
    DIAGNOSTICS
}
