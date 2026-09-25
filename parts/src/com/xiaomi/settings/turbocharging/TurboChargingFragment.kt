/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.turbocharging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragment
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.xiaomi.settings.R
import java.util.Locale

class TurboChargingFragment : PreferenceFragment(), Preference.OnPreferenceChangeListener {

    private var mBypassSwitch: SwitchPreferenceCompat? = null
    private var mBypassGameAuto: SwitchPreferenceCompat? = null
    private var mBypassCustomApps: Preference? = null
    private var mTurboSwitch: SwitchPreferenceCompat? = null
    private var mTurboCurrent: ListPreference? = null
    private var mStatusCardPref: Preference? = null

    // Status Card Views
    private var mLevelText: TextView? = null
    private var mStatusBadge: TextView? = null
    private var mProgressBar: ProgressBar? = null
    private var mTempChip: TextView? = null
    private var mVoltageChip: TextView? = null
    private var mCurrentChip: TextView? = null
    private var mConnectionText: TextView? = null

    private val mHandler = Handler(Looper.getMainLooper())
    private var mIsPolling = false

    private val mTelemetryRunnable = object : Runnable {
        override fun run() {
            updateLiveStatus()
            if (mIsPolling) {
                mHandler.postDelayed(this, 1500)
            }
        }
    }

    private val mChargingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateLiveStatus()
            syncPreferences()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.charge_panel, rootKey)

        mStatusCardPref = findPreference("charging_status_card")

        // Bypass Charging Switches
        mBypassSwitch = findPreference(ChargingUtils.PREF_BYPASS_ENABLED)
        mBypassSwitch?.onPreferenceChangeListener = this

        mBypassGameAuto = findPreference(ChargingUtils.PREF_BYPASS_GAME_AUTO)
        mBypassGameAuto?.onPreferenceChangeListener = this

        mBypassCustomApps = findPreference(ChargingUtils.PREF_BYPASS_CUSTOM_APPS)
        mBypassCustomApps?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val ctx = context ?: activity
            if (ctx != null) {
                try {
                    val intent = Intent(ctx, BypassChargingAppSelectActivity::class.java).apply {
                        action = "com.xiaomi.settings.turbocharging.BYPASS_CUSTOM_APPS"
                    }
                    ctx.startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("TurboChargingFragment", "Failed to start BypassChargingAppSelectActivity by class", e)
                    try {
                        val fallbackIntent = Intent("com.xiaomi.settings.turbocharging.BYPASS_CUSTOM_APPS").apply {
                            setPackage(ctx.packageName)
                        }
                        ctx.startActivity(fallbackIntent)
                    } catch (e2: Exception) {
                        android.util.Log.e("TurboChargingFragment", "Failed to start BypassChargingAppSelectActivity by action", e2)
                        if (activity is androidx.fragment.app.FragmentActivity) {
                            try {
                                (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager
                                    .beginTransaction()
                                    .replace(
                                        com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                                        BypassChargingAppSelectFragment()
                                    )
                                    .addToBackStack(null)
                                    .commit()
                            } catch (e3: Exception) {
                                android.util.Log.e("TurboChargingFragment", "Fragment fallback failed", e3)
                            }
                        }
                    }
                }
            }
            true
        }

        // Fast / Turbo Charging
        mTurboSwitch = findPreference(ChargingUtils.PREF_TURBO_ENABLED)
        mTurboSwitch?.onPreferenceChangeListener = this

        mTurboCurrent = findPreference(ChargingUtils.PREF_TURBO_CURRENT)
        mTurboCurrent?.onPreferenceChangeListener = this
        mTurboCurrent?.isEnabled = mTurboSwitch?.isChecked ?: false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindStatusCardViews(view)
        updateLiveStatus()
    }

    private fun bindStatusCardViews(root: View) {
        mLevelText = root.findViewById(R.id.charging_card_battery_level)
        mStatusBadge = root.findViewById(R.id.charging_card_status_badge)
        mProgressBar = root.findViewById(R.id.charging_card_progress_bar)
        mTempChip = root.findViewById(R.id.charging_card_temp_chip)
        mVoltageChip = root.findViewById(R.id.charging_card_voltage_chip)
        mCurrentChip = root.findViewById(R.id.charging_card_current_chip)
        mConnectionText = root.findViewById(R.id.charging_card_connection_text)
    }

    private var mReceiverRegistered = false

    override fun onResume() {
        super.onResume()
        if (!mReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(ChargingUtils.ACTION_CHARGING_STATE_CHANGED)
            }
            activity?.registerReceiver(mChargingReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            mReceiverRegistered = true
        }

        mIsPolling = true
        mHandler.post(mTelemetryRunnable)
        syncPreferences()
    }

    override fun onPause() {
        super.onPause()
        mIsPolling = false
        mHandler.removeCallbacks(mTelemetryRunnable)
        if (mReceiverRegistered) {
            try {
                activity?.unregisterReceiver(mChargingReceiver)
            } catch (_: Exception) {}
            mReceiverRegistered = false
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val ctx = activity ?: return false

        when (preference.key) {
            ChargingUtils.PREF_BYPASS_ENABLED -> {
                val enable = newValue as Boolean
                if (enable) {
                    val isPlugged = ChargingUtils.isUsbOnline(ctx)
                    if (!isPlugged) {
                        Toast.makeText(ctx, R.string.bypass_tile_toast_plug_in, Toast.LENGTH_SHORT).show()
                        return false
                    }
                    ChargingUtils.setBypassCharging(ctx, true)
                    Toast.makeText(ctx, R.string.bypass_tile_toast_enabled, Toast.LENGTH_SHORT).show()
                } else {
                    ChargingUtils.setBypassCharging(ctx, false)
                    Toast.makeText(ctx, R.string.bypass_tile_toast_disabled, Toast.LENGTH_SHORT).show()
                }
                updateLiveStatus()
                return true
            }

            ChargingUtils.PREF_BYPASS_GAME_AUTO -> {
                val enabled = newValue as Boolean
                mBypassCustomApps?.isEnabled = enabled
                PreferenceManager.getDefaultSharedPreferences(ctx)
                    .edit()
                    .putBoolean(ChargingUtils.PREF_BYPASS_GAME_AUTO, enabled)
                    .apply()
                ChargingUtils.notifyStateChanged(ctx)
                return true
            }

            ChargingUtils.PREF_TURBO_ENABLED -> {
                val enabled = newValue as Boolean
                mTurboCurrent?.isEnabled = enabled
                PreferenceManager.getDefaultSharedPreferences(ctx)
                    .edit()
                    .putBoolean(ChargingUtils.PREF_TURBO_ENABLED, enabled)
                    .apply()
                ChargingUtils.applySavedSettings(ctx)
                updateLiveStatus()
                return true
            }

            ChargingUtils.PREF_TURBO_CURRENT -> {
                val currentStr = newValue as String
                PreferenceManager.getDefaultSharedPreferences(ctx)
                    .edit()
                    .putString(ChargingUtils.PREF_TURBO_CURRENT, currentStr)
                    .apply()
                ChargingUtils.writeChargeCurrent(currentStr)
                updateLiveStatus()
                return true
            }
        }
        return false
    }

    private fun syncPreferences() {
        val ctx = activity ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        val manualBypass = prefs.getBoolean(ChargingUtils.PREF_BYPASS_ENABLED, false)
        mBypassSwitch?.isChecked = manualBypass

        val gameAuto = prefs.getBoolean(ChargingUtils.PREF_BYPASS_GAME_AUTO, false)
        mBypassGameAuto?.isChecked = gameAuto
        mBypassCustomApps?.isEnabled = gameAuto

        val autoApps = prefs.getStringSet(ChargingUtils.PREF_BYPASS_AUTO_APPS, emptySet()) ?: emptySet()
        val appCount = autoApps.size
        mBypassCustomApps?.summary = if (appCount == 0) {
            getString(R.string.bypass_custom_apps_summary_none)
        } else {
            getString(R.string.bypass_custom_apps_summary_count, appCount)
        }

        val turboEnabled = prefs.getBoolean(ChargingUtils.PREF_TURBO_ENABLED, true)
        mTurboSwitch?.isChecked = turboEnabled
        mTurboCurrent?.isEnabled = turboEnabled
    }

    private fun updateLiveStatus() {
        val ctx = activity ?: return
        if (mLevelText == null) {
            view?.let { bindStatusCardViews(it) }
        }
        val level = ChargingUtils.getBatteryLevel(ctx)
        val isPlugged = ChargingUtils.isUsbOnline(ctx)
        val isBypassActive = ChargingUtils.isBypassChargingActive(ctx)
        val temp = ChargingUtils.getBatteryTemperature(ctx)
        val volt = ChargingUtils.getBatteryVoltage()
        val current = ChargingUtils.getBatteryCurrentNow()
        val status = ChargingUtils.getBatteryStatus(ctx)

        // Level
        mLevelText?.text = "$level%"
        mProgressBar?.progress = level

        // Badges and colors
        val colorBypass = ContextCompat.getColor(ctx, R.color.charging_bypass_blue)
        val colorTurbo = ContextCompat.getColor(ctx, R.color.charging_turbo_amber)
        val colorGreen = ContextCompat.getColor(ctx, R.color.charging_normal_green)
        val colorGray = ContextCompat.getColor(ctx, R.color.charging_discharging_gray)

        when {
            isBypassActive && isPlugged -> {
                mStatusBadge?.text = getString(R.string.battery_status_bypass)
                mStatusBadge?.setTextColor(colorBypass)
                mProgressBar?.progressTintList = ColorStateList.valueOf(colorBypass)
                mConnectionText?.text = getString(R.string.bypass_charging_active)
            }
            isPlugged -> {
                val isTurbo = PreferenceManager.getDefaultSharedPreferences(ctx)
                    .getBoolean(ChargingUtils.PREF_TURBO_ENABLED, true)
                val turboCurrent = PreferenceManager.getDefaultSharedPreferences(ctx)
                    .getString(ChargingUtils.PREF_TURBO_CURRENT, ChargingUtils.DEFAULT_TURBO_CURRENT)

                val label = if (isTurbo && turboCurrent == "22000000") {
                    "HyperCharge 67W"
                } else if (isTurbo && turboCurrent == "7000000") {
                    "Fast Charge 30W"
                } else if (isTurbo && turboCurrent == "4500000") {
                    "Standard 18W"
                } else {
                    getString(R.string.battery_status_charging)
                }

                mStatusBadge?.text = label
                val activeColor = if (isTurbo && turboCurrent == "22000000") colorTurbo else colorGreen
                mStatusBadge?.setTextColor(activeColor)
                mProgressBar?.progressTintList = ColorStateList.valueOf(activeColor)
                mConnectionText?.text = getString(R.string.battery_connected_charger)
            }
            status == BatteryManager.BATTERY_STATUS_FULL -> {
                mStatusBadge?.text = getString(R.string.battery_status_full)
                mStatusBadge?.setTextColor(colorGreen)
                mProgressBar?.progressTintList = ColorStateList.valueOf(colorGreen)
                mConnectionText?.text = getString(R.string.battery_connected_charger)
            }
            else -> {
                mStatusBadge?.text = getString(R.string.battery_status_discharging)
                mStatusBadge?.setTextColor(colorGray)
                mProgressBar?.progressTintList = ColorStateList.valueOf(colorGreen)
                mConnectionText?.text = getString(R.string.battery_disconnected_charger)
            }
        }

        // Stats Chips
        mTempChip?.text = String.format(Locale.getDefault(), "Temp: %.1f °C", temp)
        mVoltageChip?.text = String.format(Locale.getDefault(), "Volt: %.2f V", volt)
        mCurrentChip?.text = "Rate: $current mA"
    }
}
