package com.example

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.RoverTab
import com.example.ui.RoverViewModel
import com.example.ui.components.BluetoothDeviceSheet
import com.example.ui.components.CyberGridBackground
import com.example.ui.components.ProtocolSettingsSheet
import com.example.ui.components.RoboticsTopHud
import com.example.ui.screens.CockpitScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.theme.BrushedTitanium
import com.example.ui.theme.CarbonBase
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoverLinkTheme
import com.example.ui.theme.SteelBevel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    private val viewModel: RoverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            RoverLinkTheme {
                RoverLinkApp(viewModel = viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppPausedSafetyStop()
    }
}

@Composable
fun RoverLinkApp(viewModel: RoverViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onPermissionsResult()
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshBluetoothState()
    }

    if (uiState.selectedTab == RoverTab.DIAGNOSTICS) {
        BackHandler {
            viewModel.selectTab(RoverTab.COCKPIT)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CyberGridBackground()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column(modifier = Modifier.statusBarsPadding()) {
                    RoboticsTopHud(
                        connectionState = uiState.connectionState,
                        roverBatteryPercent = uiState.esp32Telemetry.batteryPercent,
                        roverBatteryVoltage = uiState.esp32Telemetry.batteryVoltage,
                        hostBattery = uiState.hostBattery,
                        onBluetoothClick = { viewModel.openBluetoothSheet() },
                        onSettingsClick = { viewModel.openSettingsSheet() }
                    )
                }
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(SteelBevel.copy(alpha = 0.7f))
                    )
                    NavigationBar(
                        containerColor = CarbonBase.copy(alpha = 0.96f),
                        contentColor = TextPrimary,
                        windowInsets = NavigationBarDefaults.windowInsets
                    ) {
                        NavigationBarItem(
                            selected = uiState.selectedTab == RoverTab.COCKPIT,
                            onClick = { viewModel.selectTab(RoverTab.COCKPIT) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == RoverTab.COCKPIT) {
                                        Icons.Filled.Gamepad
                                    } else {
                                        Icons.Outlined.Gamepad
                                    },
                                    contentDescription = stringResource(R.string.nav_cockpit)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.nav_cockpit),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                indicatorColor = BrushedTitanium,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_tab_cockpit")
                        )

                        NavigationBarItem(
                            selected = uiState.selectedTab == RoverTab.DIAGNOSTICS,
                            onClick = { viewModel.selectTab(RoverTab.DIAGNOSTICS) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == RoverTab.DIAGNOSTICS) {
                                        Icons.Filled.Analytics
                                    } else {
                                        Icons.Outlined.Analytics
                                    },
                                    contentDescription = stringResource(R.string.nav_diagnostics)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.nav_diagnostics),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                indicatorColor = BrushedTitanium,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_tab_diagnostics")
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (uiState.selectedTab) {
                RoverTab.COCKPIT -> {
                    CockpitScreen(
                        uiState = uiState,
                        onJoystickChange = viewModel::updateJoystick,
                        onSpeedPresetSelected = viewModel::setSpeedPreset,
                        onEmergencyStop = viewModel::triggerEmergencyStop,
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                RoverTab.DIAGNOSTICS -> {
                    DiagnosticsScreen(
                        uiState = uiState,
                        onSelectLogFilter = viewModel::setLogFilter,
                        onToggleConsolePause = viewModel::toggleConsolePaused,
                        onClearLogs = viewModel::clearConsoleLogs,
                        onSendManualCommand = viewModel::sendManualCommand,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    if (uiState.isBluetoothSheetOpen) {
        BluetoothDeviceSheet(
            connectionState = uiState.connectionState,
            isBluetoothSupported = uiState.isBluetoothSupported,
            isBluetoothEnabled = uiState.isBluetoothEnabled,
            hasPermissions = uiState.hasBluetoothPermissions,
            isScanning = uiState.isScanning,
            bondedDevices = uiState.bondedDevices,
            discoveredDevices = uiState.discoveredDevices,
            onRequestPermissions = {
                permissionLauncher.launch(viewModel.requiredBluetoothPermissions())
            },
            onRequestEnableBluetooth = {
                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableIntent)
            },
            onStartScan = viewModel::startBluetoothScan,
            onStopScan = viewModel::stopBluetoothScan,
            onConnectDevice = viewModel::connectToDevice,
            onDisconnect = viewModel::disconnectBluetooth,
            onDismiss = viewModel::closeBluetoothSheet
        )
    }

    if (uiState.isSettingsSheetOpen) {
        ProtocolSettingsSheet(
            settings = uiState.settings,
            onSelectFormat = viewModel::setCommandFormat,
            onSelectMaxPwm = viewModel::setMaxPwm,
            onDeadzoneChange = viewModel::setDeadzone,
            onInvertLeftChange = viewModel::setInvertLeft,
            onInvertRightChange = viewModel::setInvertRight,
            onDismiss = viewModel::closeSettingsSheet
        )
    }
}
