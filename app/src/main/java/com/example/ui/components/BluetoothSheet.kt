package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BluetoothConnectionState
import com.example.model.BluetoothDeviceUi
import com.example.ui.theme.BrushedTitanium
import com.example.ui.theme.CarbonBase
import com.example.ui.theme.GunmetalChassis
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRed
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SteelBevel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothDeviceSheet(
    connectionState: BluetoothConnectionState,
    isBluetoothSupported: Boolean,
    isBluetoothEnabled: Boolean,
    hasPermissions: Boolean,
    isScanning: Boolean,
    bondedDevices: List<BluetoothDeviceUi>,
    discoveredDevices: List<BluetoothDeviceUi>,
    onRequestPermissions: () -> Unit,
    onRequestEnableBluetooth: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (BluetoothDeviceUi) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CarbonBase,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = NeonCyan
                    )
                    Column {
                        Text(
                            text = "ENLACE BLUETOOTH CLASSIC",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Perfil Serial SPP • ESP32-WROOM-32",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                if (connectionState is BluetoothConnectionState.Connected) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        border = BorderStroke(1.dp, NeonRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                        modifier = Modifier.testTag("bt_disconnect_button")
                    ) {
                        Text(
                            text = stringResource(R.string.bt_disconnect),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            if (connectionState is BluetoothConnectionState.Error) {
                CyberPanel(accentColor = NeonRed, contentPadding = 10.dp) {
                    Text(
                        text = connectionState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonRed
                    )
                }
            }

            when {
                !isBluetoothSupported -> {
                    CyberPanel(accentColor = NeonRed) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BluetoothDisabled,
                                contentDescription = null,
                                tint = NeonRed
                            )
                            Text(
                                text = stringResource(R.string.bt_no_adapter),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                        }
                    }
                }

                !hasPermissions -> {
                    CyberPanel(accentColor = NeonAmber) {
                        Text(
                            text = "Se requieren permisos de Bluetooth para descubrir y conectar con el módulo ESP32.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRequestPermissions,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = ObsidianVoid
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("bt_grant_permissions_button")
                        ) {
                            Text(
                                text = stringResource(R.string.bt_grant_permission),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                !isBluetoothEnabled -> {
                    CyberPanel(accentColor = NeonAmber) {
                        Text(
                            text = "El adaptador Bluetooth está apagado. Actívalo para enlazar con tu carrito ESP32.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRequestEnableBluetooth,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = ObsidianVoid
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("bt_enable_button")
                        ) {
                            Text(
                                text = stringResource(R.string.bt_enable_prompt),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { if (isScanning) onStopScan() else onStartScan() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanning) NeonAmber else NeonCyan,
                                contentColor = ObsidianVoid
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("bt_scan_button")
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = ObsidianVoid
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = stringResource(R.string.bt_scanning),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = stringResource(R.string.bt_scan_nearby),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (bondedDevices.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.bt_paired_devices).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            items(bondedDevices, key = { "bonded_${it.address}" }) { device ->
                                BluetoothDeviceRow(
                                    device = device,
                                    isConnected = (connectionState as? BluetoothConnectionState.Connected)?.address == device.address,
                                    onClick = { onConnectDevice(device) }
                                )
                            }
                        }

                        if (discoveredDevices.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.bt_discovered_devices).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonEmerald,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            items(discoveredDevices, key = { "disc_${it.address}" }) { device ->
                                BluetoothDeviceRow(
                                    device = device,
                                    isConnected = (connectionState as? BluetoothConnectionState.Connected)?.address == device.address,
                                    onClick = { onConnectDevice(device) }
                                )
                            }
                        }

                        if (bondedDevices.isEmpty() && discoveredDevices.isEmpty() && !isScanning) {
                            item {
                                CyberPanel(accentColor = SteelBevel) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = TextMuted
                                        )
                                        Text(
                                            text = stringResource(R.string.bt_empty_devices),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BluetoothDeviceRow(
    device: BluetoothDeviceUi,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isConnected -> NeonEmerald
        device.isLikelyEsp32 -> NeonCyan
        else -> SteelBevel
    }
    val shape = CutCornerShape(8.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isConnected) NeonEmerald.copy(alpha = 0.12f) else GunmetalChassis)
            .border(1.dp, borderColor.copy(alpha = 0.75f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("bt_device_${device.address}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BrushedTitanium),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isConnected -> Icons.Default.BluetoothConnected
                        device.isLikelyEsp32 -> Icons.Default.Memory
                        else -> Icons.Default.Bluetooth
                    },
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    if (device.isLikelyEsp32) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonCyan.copy(alpha = 0.18f),
                            border = BorderStroke(0.6.dp, NeonCyan)
                        ) {
                            Text(
                                text = "ESP32",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            if (device.rssi != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SignalCellularAlt,
                        contentDescription = null,
                        tint = NeonEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${device.rssi} dBm",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonEmerald
                    )
                }
            }
            Text(
                text = if (isConnected) "ACTIVO" else "CONECTAR",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isConnected) NeonEmerald else NeonCyan
            )
        }
    }
}
