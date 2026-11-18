package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.MotorCommandFormat
import com.example.model.RoverSettings
import com.example.ui.theme.BrushedTitanium
import com.example.ui.theme.CarbonBase
import com.example.ui.theme.GunmetalChassis
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SteelBevel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolSettingsSheet(
    settings: RoverSettings,
    onSelectFormat: (MotorCommandFormat) -> Unit,
    onSelectMaxPwm: (Int) -> Unit,
    onDeadzoneChange: (Float) -> Unit,
    onInvertLeftChange: (Boolean) -> Unit,
    onInvertRightChange: (Boolean) -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "PROTOCOLO Y CALIBRACIÓN ESP32",
                style = MaterialTheme.typography.titleLarge,
                color = NeonCyan
            )

            // String format selector
            CyberPanel(accentColor = NeonCyan.copy(alpha = 0.6f)) {
                Text(
                    text = "FORMATO DE TRAMA TX (MOTORES)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                MotorCommandFormat.entries.forEach { format ->
                    val selected = settings.commandFormat == format
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFormat(format) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectFormat(format) },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                            )
                            Text(
                                text = format.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) TextPrimary else TextSecondary
                            )
                        }
                        Text(
                            text = format.example,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) NeonCyan else TextMuted
                        )
                    }
                }
            }

            // PWM Scale & Deadzone
            CyberPanel(accentColor = SteelBevel) {
                Text(
                    text = "ESCALA PWM MÁXIMA",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(100, 255, 1023).forEach { pwm ->
                        val selected = settings.maxPwm == pwm
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectMaxPwm(pwm) },
                            label = {
                                Text(
                                    text = "±$pwm",
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan,
                                containerColor = GunmetalChassis,
                                labelColor = TextSecondary
                            ),
                            border = BorderStroke(1.dp, if (selected) NeonCyan else SteelBevel),
                            shape = CutCornerShape(6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ZONA MUERTA JOYSTICK",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${(settings.deadzone * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan
                    )
                }
                Slider(
                    value = settings.deadzone,
                    onValueChange = onDeadzoneChange,
                    valueRange = 0f..0.20f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = BrushedTitanium
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Invertir Motor Izquierdo (M1)", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = settings.invertLeft,
                        onCheckedChange = onInvertLeftChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianVoid,
                            checkedTrackColor = NeonCyan
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Invertir Motor Derecho (M2)", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = settings.invertRight,
                        onCheckedChange = onInvertRightChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianVoid,
                            checkedTrackColor = NeonCyan
                        )
                    )
                }
            }

            // ESP32 Telemetry RX Reference Card
            CyberPanel(accentColor = NeonEmerald.copy(alpha = 0.6f)) {
                Text(
                    text = "TELEMETRÍA RX SOPORTADA DESDE ESP32",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonEmerald
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Envía líneas terminadas en '\\n' por SerialBT.println():",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "BAT:85,V:11.8,L:180,R:175,DIST:42.5,TEMP:31.2,RSSI:-60,OBS:0",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "O depuración: [DBG] Motor calibrado OK",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
