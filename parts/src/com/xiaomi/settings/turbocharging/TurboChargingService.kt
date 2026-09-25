/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.turbocharging

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.UEventObserver
import android.util.Log
import androidx.preference.PreferenceManager
import com.xiaomi.settings.gamebar.ForegroundAppDetector

class TurboChargingService : Service() {

    private var uEventObserver: UEventObserver? = null
    private var preferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private val handler = Handler(Looper.getMainLooper())
    private var isMonitoring = false
    private var lastForegroundApp = ""

    private val monitorRunnable = object : Runnable {
        override fun run() {
            checkForegroundGameAndBypass()
            if (isMonitoring) {
                val interval = if (isScreenOn()) 1500L else 5000L
                handler.postDelayed(this, interval)
            }
        }
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    if (DEBUG) Log.d(TAG, "ACTION_POWER_CONNECTED received")
                    ChargingUtils.onPowerConnected(context)
                    updateMonitoringState()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    if (DEBUG) Log.d(TAG, "ACTION_POWER_DISCONNECTED received")
                    ChargingUtils.onPowerDisconnected(context)
                    updateMonitoringState()
                }
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> {
                    updateMonitoringState()
                    if (isMonitoring) {
                        checkForegroundGameAndBypass()
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    updateMonitoringState()
                }
                ChargingUtils.ACTION_CHARGING_STATE_CHANGED -> {
                    updateMonitoringState()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "TurboChargingService starting")

        // Register power, screen, and charging state receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(ChargingUtils.ACTION_CHARGING_STATE_CHANGED)
        }
        registerReceiver(powerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

        // UEventObserver to detect direct kernel sysfs power changes
        uEventObserver = object : UEventObserver() {
            override fun onUEvent(event: UEvent) {
                val chargerStatus = event.get("POWER_SUPPLY_ONLINE")
                if (chargerStatus == "1") {
                    ChargingUtils.onPowerConnected(this@TurboChargingService)
                    handler.post { updateMonitoringState() }
                }
            }
        }
        uEventObserver?.startObserving("DEVPATH=/sys/class/power_supply/usb")

        // Preference change listener
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                ChargingUtils.PREF_TURBO_ENABLED,
                ChargingUtils.PREF_TURBO_CURRENT -> {
                    ChargingUtils.applySavedSettings(this)
                }
                ChargingUtils.PREF_BYPASS_GAME_AUTO,
                ChargingUtils.PREF_BYPASS_AUTO_APPS,
                "game_bar_auto_apps" -> {
                    updateMonitoringState()
                    checkForegroundGameAndBypass()
                }
                ChargingUtils.PREF_BYPASS_ENABLED -> {
                    ChargingUtils.applySavedSettings(this)
                    updateMonitoringState()
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        // Initial setup
        ChargingUtils.applySavedSettings(this)
        updateMonitoringState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateMonitoringState()
        return START_STICKY
    }

    private fun isScreenOn(): Boolean {
        return try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isInteractive == true
        } catch (_: Exception) {
            true
        }
    }

    private fun updateMonitoringState() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val gameAuto = prefs.getBoolean(ChargingUtils.PREF_BYPASS_GAME_AUTO, false)
        val isPlugged = ChargingUtils.isUsbOnline(this)
        val shouldMonitor = gameAuto && isPlugged

        if (shouldMonitor && !isMonitoring) {
            isMonitoring = true
            handler.removeCallbacks(monitorRunnable)
            handler.post(monitorRunnable)
            if (DEBUG) Log.d(TAG, "Started foreground app monitoring for bypass charging")
        } else if (!shouldMonitor && isMonitoring) {
            isMonitoring = false
            handler.removeCallbacks(monitorRunnable)
            if (ChargingUtils.sAutoEngagedByGame) {
                if (DEBUG) Log.d(TAG, "Bypass monitoring stopped: reverting auto-engaged bypass")
                ChargingUtils.setBypassActiveByGame(this, false)
            }
            if (DEBUG) Log.d(TAG, "Stopped foreground app monitoring for bypass charging")
        }
    }

    private fun checkForegroundGameAndBypass() {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val gameAuto = prefs.getBoolean(ChargingUtils.PREF_BYPASS_GAME_AUTO, false)
            val isPlugged = ChargingUtils.isUsbOnline(this)

            if (!gameAuto || !isPlugged) {
                if (ChargingUtils.sAutoEngagedByGame) {
                    ChargingUtils.setBypassActiveByGame(this, false)
                }
                return
            }

            val foreground = ForegroundAppDetector.getForegroundPackageName(this)
            if (foreground.isEmpty() || foreground == "Unknown" ||
                foreground == "com.xiaomi.settings" ||
                foreground == "com.android.systemui" ||
                foreground == "android"
            ) {
                return
            }

            val bypassApps = prefs.getStringSet(ChargingUtils.PREF_BYPASS_AUTO_APPS, emptySet()) ?: emptySet()
            val gameBarApps = prefs.getStringSet("game_bar_auto_apps", emptySet()) ?: emptySet()
            val isTargetApp = bypassApps.contains(foreground) ||
                gameBarApps.contains(foreground) ||
                isAppCategoryGame(foreground)

            if (isTargetApp) {
                if (!ChargingUtils.isBypassChargingActive(this)) {
                    Log.i(TAG, "Entering game/custom app '$foreground': auto-enabling bypass charging")
                    ChargingUtils.setBypassActiveByGame(this, true)
                }
            } else {
                if (ChargingUtils.sAutoEngagedByGame) {
                    Log.i(TAG, "Exited game/custom app (foreground is '$foreground'): auto-disabling bypass charging")
                    ChargingUtils.setBypassActiveByGame(this, false)
                }
            }
            lastForegroundApp = foreground
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkForegroundGameAndBypass", e)
        }
    }

    private fun isAppCategoryGame(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            appInfo.category == ApplicationInfo.CATEGORY_GAME ||
                (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
        } catch (_: Exception) {
            false
        }
    }

    override fun onDestroy() {
        isMonitoring = false
        handler.removeCallbacks(monitorRunnable)

        try {
            unregisterReceiver(powerReceiver)
        } catch (_: Exception) {}

        uEventObserver?.stopObserving()
        uEventObserver = null

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        preferenceChangeListener?.let {
            prefs.unregisterOnSharedPreferenceChangeListener(it)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "TurboChargingService"
        private const val DEBUG = true
    }
}
