package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BluetoothConnectionState
import com.example.model.SpeedPreset
import com.example.ui.RoverUiState
import com.example.ui.components.CyberPanel
import com.example.ui.components.MotorBarGauge
import com.example.ui.components.RoboticsVirtualJoystick
import com.example.ui.theme.BrushedTitanium
import com.example.ui.theme.GunmetalChassis
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonRed
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.SteelBevel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CockpitScreen(
    uiState: RoverUiState,
    onJoystickChange: (x: Float, y: Float) -> Unit,
    onSpeedPresetSelected: (SpeedPreset) -> Unit,
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        val isWideLayout = maxWidth > 620.dp

        if (isWideLayout) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
                ) {
                    RoverTelemetryBanner(uiState = uiState)
                    MotorSpeedRow(uiState = uiState)
                    TxStringReadoutCard(uiState = uiState)
                    DriveControlsStrip(
                        selectedPreset = uiState.settings.speedPreset,
                        isEmergencyStopped = uiState.isEmergencyStopped,
                        onPresetSelected = onSpeedPresetSelected,
                        onEmergencyStop = onEmergencyStop
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    JoystickPanel(
                        uiState = uiState,
                        onJoystickChange = onJoystickChange
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoverTelemetryBanner(uiState = uiState)
                MotorSpeedRow(uiState = uiState)
                TxStringReadoutCard(uiState = uiState)
                DriveControlsStrip(
                    selectedPreset = uiState.settings.speedPreset,
                    isEmergencyStopped = uiState.isEmergencyStopped,
                    onPresetSelected = onSpeedPresetSelected,
                    onEmergencyStop = onEmergencyStop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    JoystickPanel(
                        uiState = uiState,
                        onJoystickChange = onJoystickChange
                    )
                }
            }
        }
    }
}

@Composable
private fun RoverTelemetryBanner(uiState: RoverUiState) {
    val isConnected = uiState.connectionState is BluetoothConnectionState.Connected
    val accent = if (isConnected) NeonEmerald else NeonCyan

    CyberPanel(
        accentColor = accent.copy(alpha = 0.55f),
        cornerCut = 10.dp,
        contentPadding = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_rover_chassis_1794909018143),
                contentDescription = stringResource(R.string.cd_rover_illustration),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(92.dp)
                    .height(52.dp)
                    .clip(CutCornerShape(6.dp))
                    .border(1.dp, accent.copy(alpha = 0.6f), CutCornerShape(6.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (val conn = uiState.connectionState) {
                            is BluetoothConnectionState.Connected -> "ROVER ONLINE • ${conn.deviceName}"
                            is BluetoothConnectionState.Connecting -> "ESTABLECIENDO ENLACE…"
                            is BluetoothConnectionState.Error -> "ENLACE INTERRUMPIDO"
                            is BluetoothConnectionState.Disconnected -> "MODO STANDBY • SIN ENLACE"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
                        color = if (isConnected) NeonEmerald else NeonCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VEC: ${uiState.motorOutput.directionLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "TX:${uiState.esp32Telemetry.packetsSent}  RX:${uiState.esp32Telemetry.packetsReceived}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun MotorSpeedRow(uiState: RoverUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MotorBarGauge(
            label = stringResource(R.string.motor_left_label),
            commandedSpeed = uiState.motorOutput.leftSpeed,
            reportedSpeed = uiState.esp32Telemetry.reportedLeftSpeed,
            maxPwm = uiState.settings.maxPwm,
            modifier = Modifier
                .weight(1f)
                .testTag("motor_left_gauge")
        )
        MotorBarGauge(
            label = stringResource(R.string.motor_right_label),
            commandedSpeed = uiState.motorOutput.rightSpeed,
            reportedSpeed = uiState.esp32Telemetry.reportedRightSpeed,
            maxPwm = uiState.settings.maxPwm,
            modifier = Modifier
                .weight(1f)
                .testTag("motor_right_gauge")
        )
    }
}

@Composable
private fun TxStringReadoutCard(uiState: RoverUiState) {
    val isConnected = uiState.connectionState is BluetoothConnectionState.Connected
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(6.dp))
            .background(ObsidianVoid.copy(alpha = 0.9f))
            .border(1.dp, SteelBevel, CutCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("tx_command_readout"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.tx_stream_label),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = uiState.lastSentCommand,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = if (isConnected) NeonEmerald else NeonCyan
            )
        }
        Text(
            text = if (isConnected) "SPP 20Hz" else "LOCAL PREVIEW",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = if (isConnected) NeonEmerald else TextMuted
        )
    }
}

@Composable
private fun DriveControlsStrip(
    selectedPreset: SpeedPreset,
    isEmergencyStopped: Boolean,
    onPresetSelected: (SpeedPreset) -> Unit,
    onEmergencyStop: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SpeedPreset.entries.forEach { preset ->
                val selected = preset == selectedPreset
                val presetColor = when (preset) {
                    SpeedPreset.ECO -> NeonEmerald
                    SpeedPreset.SPORT -> NeonCyan
                    SpeedPreset.TURBO -> NeonAmber
                }
                val shape = CutCornerShape(6.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(shape)
                        .background(
                            if (selected) presetColor.copy(alpha = 0.2f) else GunmetalChassis
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) presetColor else SteelBevel,
                            shape = shape
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPresetSelected(preset)
                        }
                        .testTag("speed_preset_${preset.name}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (selected) presetColor else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            ),
                            color = if (selected) TextPrimary else TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onEmergencyStop()
            },
            shape = CutCornerShape(8.dp),
            border = BorderStroke(1.5.dp, NeonRed),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEmergencyStopped) NeonRed else NeonRed.copy(alpha = 0.24f),
                contentColor = if (isEmergencyStopped) ObsidianVoid else NeonRed
            ),
            modifier = Modifier
                .height(48.dp)
                .testTag("estop_button")
        ) {
            Icon(
                imageVector = Icons.Default.Dangerous,
                contentDescription = stringResource(R.string.cd_estop_button),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.estop_label),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun JoystickPanel(
    uiState: RoverUiState,
    onJoystickChange: (x: Float, y: Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            RoboticsVirtualJoystick(
                onVectorChange = onJoystickChange
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BrushedTitanium.copy(alpha = 0.8f),
            border = BorderStroke(1.dp, SteelBevel)
        ) {
            Text(
                text = "X:%+.2f  Y:%+.2f  •  %s".format(
                    uiState.joystickX,
                    uiState.joystickY,
                    stringResource(R.string.joystick_hint)
                ),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = NeonCyan.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}
