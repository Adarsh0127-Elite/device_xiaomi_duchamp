/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.gamebar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.xiaomi.settings.gamebar.ui.theme.GameBarComposeTheme

class FpsRecordActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameBarComposeTheme {
                FpsRecordScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}
