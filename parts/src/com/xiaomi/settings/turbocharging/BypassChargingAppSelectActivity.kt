/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.turbocharging

import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.xiaomi.settings.R

class BypassChargingAppSelectActivity : CollapsingToolbarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        val bgColor = if (typedValue.resourceId != 0) {
            getColor(typedValue.resourceId)
        } else {
            typedValue.data
        }
        window.decorView.setBackgroundColor(bgColor)
        findViewById<View>(android.R.id.content)?.setBackgroundColor(bgColor)
        findViewById<View>(com.android.settingslib.collapsingtoolbar.R.id.content_parent)?.setBackgroundColor(bgColor)

        setTitle(R.string.bypass_custom_apps_title)
        if (actionBar != null) {
            actionBar?.setDisplayHomeAsUpEnabled(true)
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                    BypassChargingAppSelectFragment()
                )
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigateUp(): Boolean {
        finish()
        return true
    }
}
