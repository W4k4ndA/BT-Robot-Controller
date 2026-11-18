package com.example.domain

import com.example.model.Esp32TelemetryState
import org.json.JSONObject

data class ParsedTelemetryResult(
    val updatedState: Esp32TelemetryState,
    val isDebugMessage: Boolean,
    val debugText: String? = null
)

/**
 * Parses real-time telemetry and debug messages sent by an ESP32-WROOM-32 over Bluetooth Classic SPP.
 *
 * Supported formats:
 * 1. Key-value pairs: "BAT:84,V:11.8,L:180,R:175,DIST:42.5,TEMP:32.1,RSSI:-61,OBS:0"
 * 2. Semicolon/equals pairs: "BAT=84;L=180;R=175;US=35"
 * 3. JSON object: {"bat":84,"v":11.8,"l":180,"r":175,"dist":42.5,"rssi":-61,"msg":"Calibrado"}
 * 4. Debug lines: "[DBG] Motor OK" or free-form SerialBT debug strings
 */
object Esp32TelemetryParser {

    private val knownKeys = setOf(
        "BAT", "BATT", "BATTERY", "SOC",
        "V", "VOLT", "VOLTS", "VOLTAGE", "VBAT",
        "L", "ML", "M1", "LEFT", "PWM_L", "VEL_L",
        "R", "MR", "M2", "RIGHT", "PWM_R", "VEL_R",
        "DIST", "DISTANCE", "US", "ULTRA", "SONAR", "TOF", "CM",
        "TEMP", "T", "TEMPERATURE", "TMP",
        "AMP", "AMPS", "CURR", "CURRENT", "I",
        "OBS", "OBST", "OBSTACLE", "ALERT",
        "RSSI", "SIG", "SIGNAL", "DBM",
        "UP", "UPTIME", "TIME",
        "MSG", "DBG", "DEBUG", "LOG", "INFO", "STATUS"
    )

    fun parseLine(
        rawLine: String,
        current: Esp32TelemetryState,
        nowMs: Long = System.currentTimeMillis()
    ): ParsedTelemetryResult {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) {
            return ParsedTelemetryResult(current, isDebugMessage = false)
        }

        // Explicit debug prefix
        if (trimmed.startsWith("[DBG]", ignoreCase = true) ||
            trimmed.startsWith("DBG:", ignoreCase = true) ||
            trimmed.startsWith("DEBUG:", ignoreCase = true) ||
            trimmed.startsWith("[INFO]", ignoreCase = true) ||
            trimmed.startsWith("[ERR]", ignoreCase = true)
        ) {
            return ParsedTelemetryResult(
                updatedState = current.copy(
                    lastPacketTimeMs = nowMs,
                    packetsReceived = current.packetsReceived + 1
                ),
                isDebugMessage = true,
                debugText = trimmed
            )
        }

        // JSON payload
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return parseJson(trimmed, current, nowMs)
        }

        // Key-Value delimiter parsing (comma, semicolon, pipe, or space separated with : or =)
        val tokens = trimmed.split(',', ';', '|')
            .flatMap { segment ->
                if (segment.contains(':') || segment.contains('=')) {
                    listOf(segment.trim())
                } else {
                    segment.trim().split(' ').filter { it.isNotBlank() }
                }
            }

        val pairs = mutableMapOf<String, String>()
        for (token in tokens) {
            val sepIndex = token.indexOfAny(charArrayOf(':', '='))
            if (sepIndex > 0 && sepIndex < token.length - 1) {
                val key = token.substring(0, sepIndex).trim().uppercase()
                val value = token.substring(sepIndex + 1).trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    pairs[key] = value
                }
            }
        }

        if (pairs.isEmpty()) {
            // Treat unrecognized plain text lines from ESP32 as serial debug output
            return ParsedTelemetryResult(
                updatedState = current.copy(
                    lastPacketTimeMs = nowMs,
                    packetsReceived = current.packetsReceived + 1
                ),
                isDebugMessage = true,
                debugText = trimmed
            )
        }

        return applyPairs(pairs, current, nowMs)
    }

    private fun parseJson(
        jsonStr: String,
        current: Esp32TelemetryState,
        nowMs: Long
    ): ParsedTelemetryResult {
        return try {
            val obj = JSONObject(jsonStr)
            val pairs = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val rawKey = keys.next()
                pairs[rawKey.trim().uppercase()] = obj.optString(rawKey, "")
            }
            applyPairs(pairs, current, nowMs)
        } catch (_: Exception) {
            ParsedTelemetryResult(
                updatedState = current.copy(
                    lastPacketTimeMs = nowMs,
                    packetsReceived = current.packetsReceived + 1
                ),
                isDebugMessage = true,
                debugText = jsonStr
            )
        }
    }

    private fun applyPairs(
        pairs: Map<String, String>,
        current: Esp32TelemetryState,
        nowMs: Long
    ): ParsedTelemetryResult {
        var next = current.copy(
            lastPacketTimeMs = nowMs,
            packetsReceived = current.packetsReceived + 1
        )
        val extraSensors = current.customSensors.toMutableMap()
        var debugMsg: String? = null

        for ((key, rawVal) in pairs) {
            val cleanNum = rawVal.replace("%", "")
                .replace("V", "", ignoreCase = true)
                .replace("CM", "", ignoreCase = true)
                .replace("C", "", ignoreCase = true)
                .replace("DBM", "", ignoreCase = true)
                .replace("A", "", ignoreCase = true)
                .trim()

            when (key) {
                "BAT", "BATT", "BATTERY", "SOC" -> {
                    cleanNum.toFloatOrNull()?.toInt()?.coerceIn(0, 100)?.let {
                        next = next.copy(batteryPercent = it)
                    }
                }
                "V", "VOLT", "VOLTS", "VOLTAGE", "VBAT" -> {
                    cleanNum.toFloatOrNull()?.let {
                        next = next.copy(batteryVoltage = it)
                    }
                }
                "L", "ML", "M1", "LEFT", "PWM_L", "VEL_L" -> {
                    cleanNum.toFloatOrNull()?.toInt()?.let {
                        next = next.copy(reportedLeftSpeed = it)
                    }
                }
                "R", "MR", "M2", "RIGHT", "PWM_R", "VEL_R" -> {
                    cleanNum.toFloatOrNull()?.toInt()?.let {
                        next = next.copy(reportedRightSpeed = it)
                    }
                }
                "DIST", "DISTANCE", "US", "ULTRA", "SONAR", "TOF", "CM" -> {
                    cleanNum.toFloatOrNull()?.let {
                        next = next.copy(distanceCm = it)
                    }
                }
                "TEMP", "T", "TEMPERATURE", "TMP" -> {
                    cleanNum.toFloatOrNull()?.let {
                        next = next.copy(temperatureC = it)
                    }
                }
                "AMP", "AMPS", "CURR", "CURRENT", "I" -> {
                    cleanNum.toFloatOrNull()?.let {
                        next = next.copy(currentAmps = it)
                    }
                }
                "OBS", "OBST", "OBSTACLE", "ALERT" -> {
                    val obs = rawVal == "1" || rawVal.equals("true", true) || rawVal.equals("yes", true)
                    next = next.copy(obstacleDetected = obs)
                }
                "RSSI", "SIG", "SIGNAL", "DBM" -> {
                    cleanNum.toFloatOrNull()?.toInt()?.let {
                        next = next.copy(rssiDbm = it)
                    }
                }
                "UP", "UPTIME", "TIME" -> {
                    cleanNum.toLongOrNull()?.let {
                        next = next.copy(uptimeSeconds = it)
                    }
                }
                "MSG", "DBG", "DEBUG", "LOG", "INFO", "STATUS" -> {
                    debugMsg = rawVal
                }
                else -> {
                    if (key !in knownKeys) {
                        extraSensors[key] = rawVal
                    }
                }
            }
        }

        if (extraSensors != current.customSensors) {
            next = next.copy(customSensors = extraSensors.toMap())
        }

        return ParsedTelemetryResult(
            updatedState = next,
            isDebugMessage = debugMsg != null,
            debugText = debugMsg
        )
    }
}
