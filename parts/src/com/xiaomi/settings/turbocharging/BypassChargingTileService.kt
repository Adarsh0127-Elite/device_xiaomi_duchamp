/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.turbocharging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.xiaomi.settings.R

class BypassChargingTileService : TileService() {

    private var isReceiverRegistered = false

    private val chargingStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateTileState()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(ChargingUtils.ACTION_CHARGING_STATE_CHANGED)
            }
            registerReceiver(chargingStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            isReceiverRegistered = true
        }
        updateTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(chargingStateReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
    }

    override fun onClick() {
        super.onClick()
        val isPlugged = ChargingUtils.isUsbOnline(this)
        val isCurrentlyActive = ChargingUtils.isBypassChargingActive(this)

        if (!isPlugged && !isCurrentlyActive) {
            Toast.makeText(this, R.string.bypass_tile_toast_plug_in, Toast.LENGTH_SHORT).show()
            updateTileState()
            return
        }

        val newState = !isCurrentlyActive
        ChargingUtils.setBypassCharging(this, newState)

        val toastMsg = if (newState) {
            R.string.bypass_tile_toast_enabled
        } else {
            R.string.bypass_tile_toast_disabled
        }
        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isBypassActive = ChargingUtils.isBypassChargingActive(this)
        val isPlugged = ChargingUtils.isUsbOnline(this)

        tile.icon = Icon.createWithResource(this, R.drawable.ic_bypass_charging)
        tile.label = getString(R.string.bypass_charging_tile_label)

        when {
            isBypassActive && isPlugged -> {
                tile.state = Tile.STATE_ACTIVE
                tile.subtitle = getString(R.string.bypass_tile_active)
            }
            isPlugged -> {
                tile.state = Tile.STATE_INACTIVE
                tile.subtitle = getString(R.string.bypass_tile_connected_off)
            }
            else -> {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.subtitle = getString(R.string.bypass_tile_unplugged)
            }
        }
        tile.updateTile()
    }
}
