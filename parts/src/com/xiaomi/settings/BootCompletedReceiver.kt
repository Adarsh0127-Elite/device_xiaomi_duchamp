/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder;
import android.hardware.display.DisplayManager;
import android.util.Log
import android.view.Display;
import android.view.Display.HdrCapabilities;
import android.os.UserHandle
import com.xiaomi.settings.display.ColorService
import com.xiaomi.settings.touchsampling.TouchSamplingService
import com.xiaomi.settings.touchsampling.TouchSamplingTileService
import com.xiaomi.settings.turbocharging.TurboChargingService

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (DEBUG) Log.d(TAG, "Received boot completed intent: ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> onBootCompleted(context)
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> onLockedBootCompleted(context)
        }

        // Override HDR types
        val displayManager = context.getSystemService(DisplayManager::class.java)
        displayManager.overrideHdrTypes(
            Display.DEFAULT_DISPLAY,
            intArrayOf(
                HdrCapabilities.HDR_TYPE_DOLBY_VISION,
                HdrCapabilities.HDR_TYPE_HDR10,
                HdrCapabilities.HDR_TYPE_HLG,
                HdrCapabilities.HDR_TYPE_HDR10_PLUS
            )
        )
    }

    private fun onBootCompleted(context: Context) {
    }

    private fun onLockedBootCompleted(context: Context) {
        // Display
        ColorService.startService(context)

        // Start TurboChargingService
        val turboChargingIntent = Intent(context, TurboChargingService::class.java)
        context.startService(turboChargingIntent)

        // Start Touch Sampling Tile Service
        context.startServiceAsUser(Intent(context, TouchSamplingTileService::class.java), UserHandle.CURRENT)

        // Start Touch Sampling Service
        context.startServiceAsUser(Intent(context, TouchSamplingService::class.java), UserHandle.CURRENT)
    }

    companion object {
        private const val TAG = "BootReceiver"
        private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)
    }
}
