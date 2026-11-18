package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BluetoothConnectionState
import com.example.model.HostBatteryState
import com.example.ui.theme.AlloyHighlight
import com.example.ui.theme.BrushedTitanium
import com.example.ui.theme.CarbonBase
import com.example.ui.theme.ChromeEdge
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
import kotlin.math.abs

@Composable
fun CyberGridBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(ObsidianVoid, CarbonBase, ObsidianVoid)
            )
        )

        val step = 34.dp.toPx()
        val lineColor = NeonCyan.copy(alpha = 0.045f)
        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += step
        }

        // Subtle tactical radial accent near top-center
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NeonCyan.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.12f),
                radius = size.width * 0.75f
            ),
            radius = size.width * 0.75f,
            center = Offset(size.width * 0.5f, size.height * 0.12f)
        )
    }
}

@Composable
fun CyberPanel(
    modifier: Modifier = Modifier,
    accentColor: Color = SteelBevel,
    cornerCut: Dp = 10.dp,
    contentPadding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape: Shape = CutCornerShape(
        topStart = cornerCut,
        topEnd = 3.dp,
        bottomEnd = cornerCut,
        bottomStart = 3.dp
    )
    val metallicBrush = Brush.linearGradient(
        colors = listOf(
            BrushedTitanium.copy(alpha = 0.94f),
            GunmetalChassis.copy(alpha = 0.96f),
            CarbonBase.copy(alpha = 0.98f)
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(metallicBrush)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.85f),
                        SteelBevel.copy(alpha = 0.5f),
                        accentColor.copy(alpha = 0.45f)
                    )
                ),
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun RoboticsTopHud(
    connectionState: BluetoothConnectionState,
    roverBatteryPercent: Int?,
    roverBatteryVoltage: Float?,
    hostBattery: HostBatteryState,
    onBluetoothClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Identity + ESP32 Badge
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (connectionState) {
                                    is BluetoothConnectionState.Connected -> NeonEmerald
                                    is BluetoothConnectionState.Connecting -> NeonAmber
                                    is BluetoothConnectionState.Error -> NeonRed
                                    is BluetoothConnectionState.Disconnected -> NeonCyan
                                }
                            )
                    )
                    Text(
                        text = stringResource(R.string.app_name).uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stringResource(R.string.app_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BluetoothStatusButton(
                    connectionState = connectionState,
                    onClick = onBluetoothClick
                )

                Surface(
                    shape = CutCornerShape(8.dp),
                    color = BrushedTitanium,
                    border = BorderStroke(1.dp, SteelBevel)
                ) {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("protocol_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(R.string.cd_settings_button),
                            tint = ChromeEdge
                        )
                    }
                }
            }
        }

        // Real-Time Dual Battery Bar (ESP32 Rover + Controller Phone)
        DualBatteryIndicator(
            roverBatteryPercent = roverBatteryPercent,
            roverBatteryVoltage = roverBatteryVoltage,
            hostBattery = hostBattery,
            isConnected = connectionState is BluetoothConnectionState.Connected
        )
    }
}

@Composable
fun BluetoothStatusButton(
    connectionState: BluetoothConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "btPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btPulseAlpha"
    )

    val accentColor by animateColorAsState(
        targetValue = when (connectionState) {
            is BluetoothConnectionState.Connected -> NeonEmerald
            is BluetoothConnectionState.Connecting -> NeonAmber
            is BluetoothConnectionState.Error -> NeonRed
            is BluetoothConnectionState.Disconnected -> NeonCyan
        },
        label = "btColor"
    )

    val label = when (connectionState) {
        is BluetoothConnectionState.Connected -> connectionState.deviceName.take(12)
        is BluetoothConnectionState.Connecting -> stringResource(R.string.bt_connecting)
        is BluetoothConnectionState.Error -> "ERROR BT"
        is BluetoothConnectionState.Disconnected -> stringResource(R.string.bt_connect)
    }

    val icon = when (connectionState) {
        is BluetoothConnectionState.Connected -> Icons.Default.BluetoothConnected
        is BluetoothConnectionState.Connecting -> Icons.AutoMirrored.Filled.BluetoothSearching
        is BluetoothConnectionState.Error -> Icons.Default.BluetoothDisabled
        is BluetoothConnectionState.Disconnected -> Icons.Default.Bluetooth
    }

    val shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp, topEnd = 3.dp, bottomStart = 3.dp)

    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.22f),
                        BrushedTitanium
                    )
                )
            )
            .border(
                width = 1.2.dp,
                color = accentColor.copy(
                    alpha = if (connectionState is BluetoothConnectionState.Connecting) pulseAlpha else 0.9f
                ),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag("bluetooth_connect_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.cd_bt_button),
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DualBatteryIndicator(
    roverBatteryPercent: Int?,
    roverBatteryVoltage: Float?,
    hostBattery: HostBatteryState,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val roverColor = when {
        roverBatteryPercent == null -> TextMuted
        roverBatteryPercent <= 20 -> NeonRed
        roverBatteryPercent <= 50 -> NeonAmber
        else -> NeonEmerald
    }
    val hostColor = when {
        hostBattery.percent <= 20 -> NeonRed
        hostBattery.percent <= 45 -> NeonAmber
        else -> NeonCyan
    }

    CyberPanel(
        modifier = modifier.testTag("battery_indicator"),
        accentColor = if (isConnected) roverColor.copy(alpha = 0.7f) else SteelBevel,
        cornerCut = 8.dp,
        contentPadding = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ESP32 Rover Real-Time Battery
            Row(
                modifier = Modifier.weight(1.3f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when {
                        roverBatteryPercent != null && roverBatteryPercent <= 20 -> Icons.Default.BatteryAlert
                        roverBatteryPercent != null && roverBatteryPercent >= 85 -> Icons.Default.BatteryFull
                        else -> Icons.Default.BatteryStd
                    },
                    contentDescription = "Estado de batería del rover ESP32",
                    tint = roverColor,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.battery_rover_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        val valueText = buildString {
                            append(roverBatteryPercent?.let { "$it%" } ?: stringResource(R.string.battery_awaiting))
                            if (roverBatteryVoltage != null) {
                                append(" • %.1fV".format(roverBatteryVoltage))
                            }
                        }
                        Text(
                            text = valueText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = roverColor
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    SegmentedBatteryBar(
                        percent = roverBatteryPercent ?: 0,
                        activeColor = roverColor,
                        isAvailable = roverBatteryPercent != null
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(AlloyHighlight)
            )

            // Host Android Controller Real-Time Battery
            Row(
                modifier = Modifier.weight(0.9f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (hostBattery.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.Bolt,
                    contentDescription = "Batería del teléfono controlador",
                    tint = hostColor,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.battery_controller_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${hostBattery.percent}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = hostColor
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    SegmentedBatteryBar(
                        percent = hostBattery.percent,
                        activeColor = hostColor,
                        isAvailable = true
                    )
                }
            }
        }
    }
}

@Composable
fun SegmentedBatteryBar(
    percent: Int,
    activeColor: Color,
    isAvailable: Boolean,
    segments: Int = 8,
    modifier: Modifier = Modifier
) {
    val filledCount = if (!isAvailable) 0 else ((percent / 100f) * segments).toInt().coerceIn(0, segments)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0 until segments) {
            val active = i < filledCount
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        if (active) activeColor else AlloyHighlight.copy(alpha = 0.55f)
                    )
            )
        }
    }
}

@Composable
fun MotorBarGauge(
    label: String,
    commandedSpeed: Int,
    reportedSpeed: Int?,
    maxPwm: Int,
    modifier: Modifier = Modifier
) {
    val safeMax = maxPwm.coerceAtLeast(1)
    val ratio = (abs(commandedSpeed).toFloat() / safeMax).coerceIn(0f, 1f)
    val barColor = when {
        commandedSpeed > 0 -> NeonCyan
        commandedSpeed < 0 -> NeonMagenta
        else -> TextMuted
    }
    val polarityTag = when {
        commandedSpeed > 0 -> "FWD"
        commandedSpeed < 0 -> "REV"
        else -> "IDLE"
    }

    CyberPanel(
        modifier = modifier,
        accentColor = barColor.copy(alpha = 0.65f),
        cornerCut = 8.dp,
        contentPadding = 10.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = barColor.copy(alpha = 0.16f),
                border = BorderStroke(0.8.dp, barColor.copy(alpha = 0.6f))
            ) {
                Text(
                    text = polarityTag,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = barColor,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "%+d".format(commandedSpeed),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = if (commandedSpeed == 0) TextSecondary else barColor
            )
            if (reportedSpeed != null) {
                Text(
                    text = "RX %+d".format(reportedSpeed),
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonEmerald
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ObsidianVoid)
                .border(0.5.dp, SteelBevel, RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(7.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(barColor.copy(alpha = 0.55f), barColor)
                        )
                    )
            )
        }
    }
}
