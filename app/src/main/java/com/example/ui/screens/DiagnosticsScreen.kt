package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BluetoothConnectionState
import com.example.model.ConsoleLogEntry
import com.example.model.LogCategory
import com.example.model.TelemetrySample
import com.example.ui.RoverUiState
import com.example.ui.components.CyberPanel
import com.example.ui.theme.AlloyHighlight
import com.example.ui.theme.BrushedTitanium
import com.example.ui.theme.GunmetalChassis
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonRed
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SteelBevel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DiagnosticsScreen(
    uiState: RoverUiState,
    onSelectLogFilter: (LogCategory?) -> Unit,
    onToggleConsolePause: () -> Unit,
    onClearLogs: () -> Unit,
    onSendManualCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val telemetry = uiState.esp32Telemetry
    val effectiveRssi = telemetry.rssiDbm ?: uiState.discoveryRssiDbm

    val filteredLogs = remember(uiState.consoleLogs, uiState.selectedLogFilter) {
        val filter = uiState.selectedLogFilter
        if (filter == null) uiState.consoleLogs else uiState.consoleLogs.filter { it.category == filter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Bluetooth Signal Strength & Link Health
        item {
            SignalDiagnosticsCard(
                connectionState = uiState.connectionState,
                rssiDbm = effectiveRssi,
                packetsSent = telemetry.packetsSent,
                packetsReceived = telemetry.packetsReceived,
                lastPacketTimeMs = telemetry.lastPacketTimeMs
            )
        }

        // 2. Real-time Motor Speed Feedback + Dual Waveform Chart
        item {
            MotorVelocityTelemetryCard(
                txLeft = uiState.motorOutput.leftSpeed,
                txRight = uiState.motorOutput.rightSpeed,
                rxLeft = telemetry.reportedLeftSpeed,
                rxRight = telemetry.reportedRightSpeed,
                maxPwm = uiState.settings.maxPwm,
                history = uiState.telemetryHistory
            )
        }

        // 3. Real-time ESP32 Sensors Grid
        item {
            Esp32SensorsCard(uiState = uiState)
        }

        // 4. ESP32 Debug & Serial Console
        item {
            Esp32DebugConsoleCard(
                logs = filteredLogs,
                selectedFilter = uiState.selectedLogFilter,
                isPaused = uiState.isConsolePaused,
                isConnected = uiState.connectionState is BluetoothConnectionState.Connected,
                onSelectFilter = onSelectLogFilter,
                onTogglePause = onToggleConsolePause,
                onClearLogs = onClearLogs,
                onSendManualCommand = onSendManualCommand
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun SignalDiagnosticsCard(
    connectionState: BluetoothConnectionState,
    rssiDbm: Int?,
    packetsSent: Long,
    packetsReceived: Long,
    lastPacketTimeMs: Long?
) {
    val isConnected = connectionState is BluetoothConnectionState.Connected
    val signalQuality = when {
        !isConnected && rssiDbm == null -> 0
        rssiDbm == null -> 3 // Connected via SPP without explicit RSSI telemetry yet
        rssiDbm >= -55 -> 4
        rssiDbm >= -70 -> 3
        rssiDbm >= -85 -> 2
        else -> 1
    }
    val signalColor = when (signalQuality) {
        4, 3 -> NeonEmerald
        2 -> NeonAmber
        1 -> NeonRed
        else -> TextMuted
    }
    val signalLabel = when {
        !isConnected -> "DESCONECTADO"
        rssiDbm != null -> "$rssiDbm dBm"
        else -> "SPP ACTIVO"
    }

    CyberPanel(
        modifier = Modifier.testTag("diag_signal_card"),
        accentColor = if (isConnected) signalColor else SteelBevel
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SignalCellularAlt,
                    contentDescription = null,
                    tint = signalColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.diag_signal_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Text(
                text = signalLabel,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = signalColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Neon 4-bar signal meter
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                listOf(10.dp, 15.dp, 20.dp, 26.dp).forEachIndexed { idx, h ->
                    val active = idx < signalQuality
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(h)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (active) signalColor else AlloyHighlight.copy(alpha = 0.5f))
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = when (connectionState) {
                            is BluetoothConnectionState.Connected -> "${connectionState.deviceName} (${connectionState.address})"
                            is BluetoothConnectionState.Connecting -> "Conectando a ${connectionState.deviceName}…"
                            is BluetoothConnectionState.Error -> connectionState.message
                            is BluetoothConnectionState.Disconnected -> "Sin dispositivo enlazado"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val rxStatus = if (lastPacketTimeMs != null) {
                        val ageSec = ((System.currentTimeMillis() - lastPacketTimeMs).coerceAtLeast(0L)) / 1000.0
                        "Última trama RX hace %.1fs".format(ageSec)
                    } else {
                        "Sin tramas RX recibidas del ESP32"
                    }
                    Text(
                        text = rxStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniStatBadge(
                label = "PAQUETES TX",
                value = packetsSent.toString(),
                color = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            MiniStatBadge(
                label = "PAQUETES RX",
                value = packetsReceived.toString(),
                color = NeonEmerald,
                modifier = Modifier.weight(1f)
            )
            MiniStatBadge(
                label = "CALIDAD RF",
                value = when (signalQuality) {
                    4 -> "ÓPTIMA"
                    3 -> "ALTA"
                    2 -> "MEDIA"
                    1 -> "DÉBIL"
                    else -> "--"
                },
                color = signalColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MotorVelocityTelemetryCard(
    txLeft: Int,
    txRight: Int,
    rxLeft: Int?,
    rxRight: Int?,
    maxPwm: Int,
    history: List<TelemetrySample>
) {
    CyberPanel(
        modifier = Modifier.testTag("diag_motors_card"),
        accentColor = NeonCyan.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.diag_motors_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Text(
                text = "PWM ±$maxPwm",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniStatBadge(
                label = "M1 IZQ (RX / TX)",
                value = "${rxLeft?.toString() ?: "--"} / $txLeft",
                color = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            MiniStatBadge(
                label = "M2 DER (RX / TX)",
                value = "${rxRight?.toString() ?: "--"} / $txRight",
                color = NeonMagenta,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Real-time dual-channel telemetry sparkline chart
        MotorWaveformCanvas(
            history = history,
            maxPwm = maxPwm.coerceAtLeast(1),
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ObsidianVoid)
                .border(1.dp, SteelBevel, RoundedCornerShape(6.dp))
                .padding(6.dp)
        )
    }
}

@Composable
private fun MotorWaveformCanvas(
    history: List<TelemetrySample>,
    maxPwm: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val midY = size.height / 2f
        drawLine(
            color = SteelBevel.copy(alpha = 0.55f),
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = AlloyHighlight.copy(alpha = 0.4f),
            start = Offset(0f, size.height * 0.25f),
            end = Offset(size.width, size.height * 0.25f),
            strokeWidth = 0.7.dp.toPx()
        )
        drawLine(
            color = AlloyHighlight.copy(alpha = 0.4f),
            start = Offset(0f, size.height * 0.75f),
            end = Offset(size.width, size.height * 0.75f),
            strokeWidth = 0.7.dp.toPx()
        )

        if (history.size < 2) return@Canvas

        val leftPath = Path()
        val rightPath = Path()
        val stepX = size.width / (history.size - 1).coerceAtLeast(1)

        history.forEachIndexed { idx, sample ->
            val x = idx * stepX
            val lVal = (sample.rxLeft ?: sample.txLeft).toFloat().coerceIn(-maxPwm.toFloat(), maxPwm.toFloat())
            val rVal = (sample.rxRight ?: sample.txRight).toFloat().coerceIn(-maxPwm.toFloat(), maxPwm.toFloat())

            val yL = midY - (lVal / maxPwm) * (midY * 0.88f)
            val yR = midY - (rVal / maxPwm) * (midY * 0.88f)

            if (idx == 0) {
                leftPath.moveTo(x, yL)
                rightPath.moveTo(x, yR)
            } else {
                leftPath.lineTo(x, yL)
                rightPath.lineTo(x, yR)
            }
        }

        drawPath(
            path = leftPath,
            color = NeonCyan,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = rightPath,
            color = NeonMagenta,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun Esp32SensorsCard(uiState: RoverUiState) {
    val t = uiState.esp32Telemetry
    val obstacleAlert = t.obstacleDetected == true || (t.distanceCm != null && t.distanceCm < 20f)

    CyberPanel(
        modifier = Modifier.testTag("diag_sensors_card"),
        accentColor = if (obstacleAlert) NeonRed else NeonEmerald.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = NeonEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.diag_sensors_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            if (obstacleAlert) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = NeonRed.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, NeonRed)
                ) {
                    Text(
                        text = "OBSTÁCULO",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = NeonRed,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SensorTile(
                icon = Icons.Default.Sensors,
                label = "ULTRASÓNICO",
                value = t.distanceCm?.let { "%.1f cm".format(it) } ?: "-- cm",
                color = if (obstacleAlert) NeonRed else NeonCyan,
                modifier = Modifier.weight(1f)
            )
            SensorTile(
                icon = Icons.Default.Thermostat,
                label = "TEMPERATURA",
                value = t.temperatureC?.let { "%.1f °C".format(it) } ?: "-- °C",
                color = if ((t.temperatureC ?: 0f) > 65f) NeonAmber else NeonEmerald,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SensorTile(
                icon = Icons.Default.Warning,
                label = "CORRIENTE / VOLT",
                value = buildString {
                    append(t.batteryVoltage?.let { "%.2fV".format(it) } ?: "--V")
                    if (t.currentAmps != null) {
                        append(" • %.2fA".format(t.currentAmps))
                    }
                },
                color = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            SensorTile(
                icon = Icons.Default.Timer,
                label = "UPTIME / OBS",
                value = buildString {
                    append(t.uptimeSeconds?.let { "${it}s" } ?: "--s")
                    append(" • ")
                    append(
                        when (t.obstacleDetected) {
                            true -> "ALERTA"
                            false -> "LIBRE"
                            null -> "--"
                        }
                    )
                },
                color = if (t.obstacleDetected == true) NeonRed else NeonEmerald,
                modifier = Modifier.weight(1f)
            )
        }

        if (t.customSensors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SENSORES ADICIONALES REPORTADOS",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                t.customSensors.forEach { (key, valStr) ->
                    MiniStatBadge(
                        label = key,
                        value = valStr,
                        color = NeonCyan
                    )
                }
            }
        } else if (t.packetsReceived == 0L) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.diag_awaiting_telemetry),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun SensorTile(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(CutCornerShape(6.dp))
            .background(GunmetalChassis)
            .border(0.8.dp, color.copy(alpha = 0.45f), CutCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = TextSecondary,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MiniStatBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(ObsidianVoid.copy(alpha = 0.85f))
            .border(0.7.dp, SteelBevel, RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = TextMuted,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun Esp32DebugConsoleCard(
    logs: List<ConsoleLogEntry>,
    selectedFilter: LogCategory?,
    isPaused: Boolean,
    isConnected: Boolean,
    onSelectFilter: (LogCategory?) -> Unit,
    onTogglePause: () -> Unit,
    onClearLogs: () -> Unit,
    onSendManualCommand: (String) -> Unit
) {
    var manualInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size, isPaused) {
        if (!isPaused && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.lastIndex)
        }
    }

    CyberPanel(
        modifier = Modifier.testTag("diag_console_card"),
        accentColor = SteelBevel
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.diag_console_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Row {
                IconButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("console_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Reanudar consola" else "Pausar consola",
                        tint = if (isPaused) NeonAmber else NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("console_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = stringResource(R.string.diag_clear_logs),
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterPill(
                label = "TODO",
                selected = selectedFilter == null,
                color = NeonCyan,
                onClick = { onSelectFilter(null) }
            )
            listOf(
                LogCategory.RX to NeonEmerald,
                LogCategory.TX to NeonCyan,
                LogCategory.DEBUG to NeonAmber,
                LogCategory.ERR to NeonRed
            ).forEach { (cat, color) ->
                FilterPill(
                    label = cat.tag,
                    selected = selectedFilter == cat,
                    color = color,
                    onClick = { onSelectFilter(cat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Monospace Terminal Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ObsidianVoid)
                .border(1.dp, SteelBevel, RoundedCornerShape(6.dp))
                .padding(8.dp)
        ) {
            if (logs.isEmpty()) {
                Text(
                    text = "// Sin mensajes registrados en la consola.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(logs, key = { it.id }) { entry ->
                        val catColor = when (entry.category) {
                            LogCategory.RX -> NeonEmerald
                            LogCategory.TX -> NeonCyan
                            LogCategory.DEBUG -> NeonAmber
                            LogCategory.SYS -> TextSecondary
                            LogCategory.ERR -> NeonRed
                        }
                        Text(
                            text = "${entry.timestampFormatted} [${entry.category.tag}] ${entry.message}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.5.sp
                            ),
                            color = catColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Manual serial command sender
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = manualInput,
                onValueChange = { manualInput = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.diag_send_cmd_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.labelMedium.copy(color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = SteelBevel,
                    focusedContainerColor = ObsidianVoid,
                    unfocusedContainerColor = ObsidianVoid
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (manualInput.isNotBlank()) {
                            onSendManualCommand(manualInput)
                            manualInput = ""
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("console_command_input")
            )

            Surface(
                shape = CutCornerShape(8.dp),
                color = if (isConnected) NeonCyan.copy(alpha = 0.2f) else BrushedTitanium,
                border = BorderStroke(1.dp, if (isConnected) NeonCyan else SteelBevel)
            ) {
                IconButton(
                    onClick = {
                        if (manualInput.isNotBlank()) {
                            onSendManualCommand(manualInput)
                            manualInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("console_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar comando manual",
                        tint = if (isConnected) NeonCyan else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = CutCornerShape(5.dp),
        color = if (selected) color.copy(alpha = 0.22f) else GunmetalChassis,
        border = BorderStroke(1.dp, if (selected) color else SteelBevel),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (selected) color else TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
