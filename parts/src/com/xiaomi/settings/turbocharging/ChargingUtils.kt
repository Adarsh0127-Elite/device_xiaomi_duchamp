/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.turbocharging

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.preference.PreferenceManager
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

object ChargingUtils {
    private const val TAG = "ChargingUtils"
    private const val DEBUG = true

    // Sysfs and Proc Node Definitions for Xiaomi / MediaTek (duchamp)
    const val PROC_CURRENT_CMD = "/proc/mtk_battery_cmd/current_cmd"
    const val PROC_EN_POWER_PATH = "/proc/mtk_battery_cmd/en_power_path"
    const val NODE_INPUT_SUSPEND = "/sys/class/power_supply/battery/input_suspend"
    const val NODE_INPUT_SUSPEND_FALLBACK = "/sys/class/power_supply/battery/charging_enabled"
    const val NODE_CHARGE_CURRENT = "/sys/class/power_supply/battery/constant_charge_current"
    const val NODE_USB_ONLINE = "/sys/class/power_supply/usb/online"
    const val NODE_BATTERY_TEMP = "/sys/class/power_supply/battery/temp"
    const val NODE_BATTERY_CAPACITY = "/sys/class/power_supply/battery/capacity"
    const val NODE_BATTERY_STATUS = "/sys/class/power_supply/battery/status"
    const val NODE_BATTERY_CURRENT = "/sys/class/power_supply/battery/current_now"
    const val NODE_BATTERY_VOLTAGE = "/sys/class/power_supply/battery/voltage_now"

    // Candidate current_cmd nodes for MediaTek charger framework
    private val CANDIDATE_CURRENT_CMD_NODES = listOf(
        PROC_CURRENT_CMD,
        "/proc/mtk_battery_cmd/current_cmd"
    )

    // Candidate power path sysfs/proc nodes
    private val CANDIDATE_POWER_PATH_NODES = listOf(
        PROC_EN_POWER_PATH,
        "/proc/mtk_battery_cmd/en_power_path",
        "/sys/devices/platform/charger/en_power_path"
    )

    // Candidate suspend sysfs nodes
    private val CANDIDATE_SUSPEND_NODES = listOf(
        NODE_INPUT_SUSPEND,
        "/sys/devices/platform/11f01000.i2c/i2c-5/5-0034/11f01000.i2c:mt6375@34:mtk-gauge/power_supply/battery/input_suspend",
        "/sys/devices/platform/11f01000.i2c/i2c-5/5-0034/11f01000.i2c:mt6375@34:chg/power_supply/battery/input_suspend",
        "/sys/devices/platform/11b72000.i2c/i2c-10/10-0055/power_supply/battery/input_suspend",
        "/sys/devices/platform/11b72000.i2c/i2c-10/10-006f/power_supply/battery/input_suspend"
    )

    private val CANDIDATE_CHARGING_ENABLED_NODES = listOf(
        NODE_INPUT_SUSPEND_FALLBACK,
        "/sys/class/power_supply/battery/battery_charging_enabled",
        "/sys/devices/platform/11f01000.i2c/i2c-5/5-0034/11f01000.i2c:mt6375@34:mtk-gauge/power_supply/battery/charging_enabled",
        "/sys/devices/platform/11f01000.i2c/i2c-5/5-0034/11f01000.i2c:mt6375@34:chg/power_supply/battery/charging_enabled"
    )

    private val CANDIDATE_CURRENT_NODES = listOf(
        NODE_CHARGE_CURRENT,
        "/sys/devices/platform/11f01000.i2c/i2c-5/5-0034/11f01000.i2c:mt6375@34:mtk-gauge/power_supply/battery/constant_charge_current",
        "/sys/devices/platform/11f01000.i2c/i2c-5/5-0034/11f01000.i2c:mt6375@34:chg/power_supply/battery/constant_charge_current"
    )

    private val CANDIDATE_USB_ONLINE_NODES = listOf(
        NODE_USB_ONLINE,
        "/sys/devices/platform/charger/power_supply/usb/online",
        "/sys/class/power_supply/charger/online",
        "/sys/class/power_supply/primary_chg/online"
    )

    // Preference Keys
    const val PREF_BYPASS_ENABLED = "bypass_charging_enable"
    const val PREF_BYPASS_GAME_AUTO = "bypass_game_auto"
    const val PREF_BYPASS_CUSTOM_APPS = "bypass_custom_apps"
    const val PREF_BYPASS_AUTO_APPS = "bypass_auto_apps"
    const val PREF_TURBO_ENABLED = "turbo_enable"
    const val PREF_TURBO_CURRENT = "turbo_current"

    // Internal tracker if game/custom app auto-triggered bypass
    @Volatile
    var sAutoEngagedByGame = false

    @Volatile
    private var sBypassActive: Boolean = false

    // Action broadcast for real-time UI & Tile synchronization
    const val ACTION_CHARGING_STATE_CHANGED =
        "com.xiaomi.settings.turbocharging.ACTION_CHARGING_STATE_CHANGED"

    // Default Values
    const val DEFAULT_TURBO_CURRENT = "22000000" // 67W HyperCharge
    const val DEFAULT_NORMAL_CURRENT = "21000000"

    fun getResolvedSuspendNode(): String? {
        return CANDIDATE_SUSPEND_NODES.firstOrNull { File(it).exists() }
    }

    fun getResolvedChargingEnabledNode(): String? {
        return CANDIDATE_CHARGING_ENABLED_NODES.firstOrNull { File(it).exists() }
    }

    fun getResolvedChargeCurrentNode(): String? {
        return CANDIDATE_CURRENT_NODES.firstOrNull { File(it).exists() }
    }

    fun isBypassChargingSupported(): Boolean {
        return getResolvedChargeCurrentNode() != null || File(PROC_CURRENT_CMD).exists()
    }

    fun isBypassChargingActive(context: Context? = null): Boolean {
        if (sAutoEngagedByGame) return true
        if (context != null) {
            sBypassActive = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_BYPASS_ENABLED, false)
        }
        return sBypassActive
    }

    /**
     * Ensures input_suspend is ALWAYS 0 and power path is 1.
     * Prevents disconnecting charger current from the motherboard.
     */
    fun ensureInputNotSuspended() {
        for (node in CANDIDATE_SUSPEND_NODES) {
            if (File(node).exists()) {
                writeOneLine(node, "0")
            }
        }
        for (node in CANDIDATE_POWER_PATH_NODES) {
            if (File(node).exists()) {
                writeOneLine(node, "1")
            }
        }
    }

    /**
     * Applies hardware-level bypass charging state.
     * When enable=true:
     *   - Power path is forced ON so USB powers the system directly.
     *   - input_suspend is forced 0 so USB input current is NOT disconnected.
     *   - /proc/mtk_battery_cmd/current_cmd is sent "0 1" to disable charging to battery cells.
     *   - constant_charge_current is set to 0 to prevent charging.
     *   - charging_enabled is set to 0.
     * When enable=false:
     *   - /proc/mtk_battery_cmd/current_cmd is sent "0 0" to resume charging.
     *   - charging_enabled is set to 1.
     *   - constant_charge_current is restored to configured turbo/normal wattage.
     */
    @Synchronized
    fun applyBypassHardware(context: Context, enable: Boolean): Boolean {
        var success = false

        if (enable) {
            // 1. Ensure system power path is active so charger directly powers the motherboard
            for (node in CANDIDATE_POWER_PATH_NODES) {
                if (File(node).exists()) {
                    writeOneLine(node, "1")
                }
            }

            // 2. Guarantee input_suspend is ALWAYS 0 so USB input current is NOT disconnected
            for (node in CANDIDATE_SUSPEND_NODES) {
                if (File(node).exists()) {
                    writeOneLine(node, "0")
                }
            }

            // 3. Command MediaTek charger framework to stop charging battery cells
            // Format: echo [cmd] [val] > current_cmd
            // cmd 0: charging disable (1) / enable (0)
            // cmd 1: usb unlimited enable (1) / disable (0)
            for (node in CANDIDATE_CURRENT_CMD_NODES) {
                if (File(node).exists()) {
                    if (writeOneLine(node, "0 1")) {
                        success = true
                    }
                    writeOneLine(node, "1 1")
                }
            }

            // 4. Set constant charge current to 0 so battery charging algorithm supplies 0 mA
            if (writeChargeCurrent("0")) {
                success = true
            }

            // 5. Disable charging on any standard charging_enabled nodes if present
            for (node in CANDIDATE_CHARGING_ENABLED_NODES) {
                if (File(node).exists()) {
                    writeOneLine(node, "0")
                }
            }

            Log.i(TAG, "Hardware bypass ACTIVATED: current_cmd='0 1', charge_current=0, input_suspend=0 (success=$success)")
        } else {
            // 1. Command MediaTek charger framework to resume battery charging
            for (node in CANDIDATE_CURRENT_CMD_NODES) {
                if (File(node).exists()) {
                    if (writeOneLine(node, "0 0")) {
                        success = true
                    }
                    writeOneLine(node, "1 0")
                }
            }

            // 2. Ensure system power path is active
            for (node in CANDIDATE_POWER_PATH_NODES) {
                if (File(node).exists()) {
                    writeOneLine(node, "1")
                }
            }

            // 3. Ensure input is not suspended
            for (node in CANDIDATE_SUSPEND_NODES) {
                if (File(node).exists()) {
                    writeOneLine(node, "0")
                }
            }

            // 4. Re-enable charging on charging_enabled nodes
            for (node in CANDIDATE_CHARGING_ENABLED_NODES) {
                if (File(node).exists()) {
                    writeOneLine(node, "1")
                }
            }

            // 5. Restore configured turbo or normal current
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val turboEnabled = prefs.getBoolean(PREF_TURBO_ENABLED, true)
            val targetCurrent = if (turboEnabled) {
                prefs.getString(PREF_TURBO_CURRENT, DEFAULT_TURBO_CURRENT) ?: DEFAULT_TURBO_CURRENT
            } else {
                DEFAULT_NORMAL_CURRENT
            }
            if (writeChargeCurrent(targetCurrent)) {
                success = true
            }

            Log.i(TAG, "Hardware bypass DEACTIVATED: current_cmd='0 0', charge_current=$targetCurrent (success=$success)")
        }

        return success
    }

    /**
     * Manual toggle for bypass charging (via Settings UI or QS Tile).
     * Saves user preference to SharedPreferences and applies hardware state.
     */
    @Synchronized
    fun setBypassCharging(context: Context, enable: Boolean): Boolean {
        if (DEBUG) Log.i(TAG, "setBypassCharging (manual): enable=$enable")
        sBypassActive = enable
        if (!enable) {
            sAutoEngagedByGame = false
        }
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(PREF_BYPASS_ENABLED, enable)
            .apply()

        val success = applyBypassHardware(context, enable)
        notifyStateChanged(context)
        return success
    }

    /**
     * Automatic trigger by foreground game or custom selected app.
     * Does NOT overwrite the user's manual PREF_BYPASS_ENABLED setting.
     */
    @Synchronized
    fun setBypassActiveByGame(context: Context, enable: Boolean): Boolean {
        if (DEBUG) Log.i(TAG, "setBypassActiveByGame: enable=$enable, current sAutoEngagedByGame=$sAutoEngagedByGame")
        sAutoEngagedByGame = enable
        val manualEnabled = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PREF_BYPASS_ENABLED, false)

        val shouldBeBypass = enable || manualEnabled
        val success = applyBypassHardware(context, shouldBeBypass)
        notifyStateChanged(context)
        return success
    }

    fun isUsbOnline(context: Context? = null): Boolean {
        // 1. Physical USB connection check via power_supply/usb/type
        val usbType = readOneLine("/sys/class/power_supply/usb/type")?.trim()
        if (!usbType.isNullOrEmpty() &&
            !usbType.equals("Unknown", ignoreCase = true) &&
            !usbType.equals("None", ignoreCase = true)
        ) {
            return true
        }

        // 2. Candidate USB online nodes
        for (node in CANDIDATE_USB_ONLINE_NODES) {
            val state = readOneLine(node)?.trim()
            if (state == "1") return true
        }

        // 3. Type-C port power role
        val typecRole = readOneLine("/sys/class/typec/port0/power_role")?.trim()
        if (typecRole?.contains("sink", ignoreCase = true) == true) {
            return true
        }

        // 4. Android BatteryManager plugged extra
        if (context != null) {
            try {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                if (intent != null) {
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                    if (plugged != 0) return true
                }
            } catch (_: Exception) {}
        }

        return false
    }

    fun getBatteryLevel(context: Context): Int {
        if (File(NODE_BATTERY_CAPACITY).exists()) {
            val cap = readOneLine(NODE_BATTERY_CAPACITY)?.trim()?.toIntOrNull()
            if (cap != null && cap in 0..100) return cap
        }
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else 0
        } catch (_: Exception) {
            0
        }
    }

    fun getBatteryTemperature(context: Context? = null): Float {
        if (File(NODE_BATTERY_TEMP).exists()) {
            val raw = readOneLine(NODE_BATTERY_TEMP)?.trim()?.toIntOrNull()
            if (raw != null) {
                val temp = if (raw > 1000) raw / 10f else if (raw > 100) raw / 10f else raw.toFloat()
                if (temp in -30f..110f) return temp
            }
        }
        if (context != null) {
            try {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
                if (temp != Int.MIN_VALUE) {
                    return temp / 10f
                }
            } catch (_: Exception) {}
        }
        return 25.0f
    }

    fun getBatteryVoltage(): Float {
        if (File(NODE_BATTERY_VOLTAGE).exists()) {
            val raw = readOneLine(NODE_BATTERY_VOLTAGE)?.trim()?.toIntOrNull()
            if (raw != null) {
                return when {
                    raw > 1_000_000 -> raw / 1_000_000f
                    raw > 1_000 -> raw / 1_000f
                    else -> raw.toFloat()
                }
            }
        }
        return 4.0f
    }

    fun getBatteryCurrentNow(): Int {
        if (File(NODE_BATTERY_CURRENT).exists()) {
            val raw = readOneLine(NODE_BATTERY_CURRENT)?.trim()?.toIntOrNull()
            if (raw != null) {
                return if (kotlin.math.abs(raw) > 10_000) raw / 1_000 else raw
            }
        }
        return 0
    }

    fun getBatteryStatus(context: Context): Int {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        } catch (_: Exception) {
            BatteryManager.BATTERY_STATUS_UNKNOWN
        }
    }

    fun applySavedSettings(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val isPlugged = isUsbOnline(context)
        val bypassActive = prefs.getBoolean(PREF_BYPASS_ENABLED, false) || sAutoEngagedByGame
        sBypassActive = prefs.getBoolean(PREF_BYPASS_ENABLED, false)

        if (isPlugged && bypassActive) {
            applyBypassHardware(context, true)
        } else {
            applyBypassHardware(context, false)
        }
    }

    fun writeChargeCurrent(value: String): Boolean {
        var anyWritten = false
        val node = getResolvedChargeCurrentNode()
        if (node != null && writeOneLine(node, value)) {
            anyWritten = true
        }
        for (n in CANDIDATE_CURRENT_NODES) {
            if (n != node && File(n).exists()) {
                if (writeOneLine(n, value)) {
                    anyWritten = true
                }
            }
        }
        return anyWritten
    }

    /**
     * Boot initialization: ensures normal charging and applies saved settings.
     */
    fun onBoot(context: Context) {
        Log.i(TAG, "onBoot: ensuring normal charging and applying settings")
        ensureInputNotSuspended()
        applySavedSettings(context)
    }

    /**
     * Called when USB disconnected.
     * Resets auto-game engagement and restores normal charger profile without wiping manual setting.
     */
    fun onPowerDisconnected(context: Context) {
        if (DEBUG) Log.i(TAG, "Power disconnected")
        sAutoEngagedByGame = false
        applyBypassHardware(context, false)
        ensureInputNotSuspended()
        notifyStateChanged(context)
    }

    /**
     * Called when USB connected:
     * Re-applies charge profile according to saved preferences.
     */
    fun onPowerConnected(context: Context) {
        if (DEBUG) Log.i(TAG, "Power connected, applying charge profile")
        ensureInputNotSuspended()
        applySavedSettings(context)
        notifyStateChanged(context)
    }

    fun resetBypass(context: Context) {
        sAutoEngagedByGame = false
        sBypassActive = false
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(PREF_BYPASS_ENABLED, false)
            .apply()
        applyBypassHardware(context, false)
        notifyStateChanged(context)
    }

    fun notifyStateChanged(context: Context) {
        try {
            val intent = Intent(ACTION_CHARGING_STATE_CHANGED)
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
    }

    private fun readOneLine(path: String): String? {
        val file = File(path)
        if (!file.exists() || !file.canRead()) return null
        return try {
            BufferedReader(FileReader(file)).use { it.readLine() }
        } catch (e: Exception) {
            if (DEBUG) Log.w(TAG, "Could not read $path: ${e.message}")
            null
        }
    }

    private fun writeOneLine(path: String, value: String): Boolean {
        val file = File(path)
        if (!file.exists()) {
            return false
        }
        return try {
            BufferedWriter(FileWriter(file)).use {
                it.write(value)
                it.newLine()
                it.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write '$value' to $path: ${e.message}")
            false
        }
    }
}
