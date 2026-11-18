package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.model.HostBatteryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors the Android controller device's real-time battery level and charging status
 * using system [BatteryManager] broadcasts.
 */
class HostBatteryMonitor(context: Context) {

    private val appContext = context.applicationContext
    private val _batteryState = MutableStateFlow(readInitialBattery())
    val batteryState: StateFlow<HostBatteryState> = _batteryState.asStateFlow()

    private var isRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                _batteryState.value = extractState(intent)
            }
        }
    }

    fun start() {
        if (!isRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val sticky = appContext.registerReceiver(receiver, filter)
            sticky?.let { _batteryState.value = extractState(it) }
            isRegistered = true
        }
    }

    fun stop() {
        if (isRegistered) {
            try {
                appContext.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
            }
            isRegistered = false
        }
    }

    private fun readInitialBattery(): HostBatteryState {
        val manager = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val pct = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 0..100 } ?: 100
        return HostBatteryState(percent = pct)
    }

    private fun extractState(intent: Intent): HostBatteryState {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val percent = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).toInt().coerceIn(0, 100)
        } else {
            _batteryState.value.percent
        }
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)

        return HostBatteryState(
            percent = percent,
            isCharging = isCharging,
            voltageMv = voltageMv,
            temperatureTenthsC = tempTenths
        )
    }
}
