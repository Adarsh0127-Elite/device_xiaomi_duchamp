/*
 * SPDX-FileCopyrightText: 2025-2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.gamebar

import android.content.SharedPreferences
import android.graphics.Color
import android.util.Log

object GameBarPrefsUtils {
    private const val TAG = "GameBarPrefsUtils"

    fun parseColorSafe(value: Any?, defaultColor: Int): Int {
        if (value == null) return defaultColor
        return when (value) {
            is Int -> value
            is Number -> value.toInt()
            is String -> {
                val str = value.trim()
                if (str.isEmpty()) return defaultColor
                try {
                    Color.parseColor(str)
                } catch (e: Exception) {
                    try {
                        str.toLong().toInt()
                    } catch (e2: Exception) {
                        try {
                            if (str.startsWith("0x", ignoreCase = true)) {
                                java.lang.Long.parseLong(str.substring(2), 16).toInt()
                            } else {
                                defaultColor
                            }
                        } catch (e3: Exception) {
                            defaultColor
                        }
                    }
                }
            }
            else -> defaultColor
        }
    }

    fun parseIntSafe(value: Any?, defaultValue: Int): Int {
        if (value == null) return defaultValue
        return when (value) {
            is Int -> value
            is Number -> value.toInt()
            is String -> {
                val str = value.trim()
                str.toIntOrNull() ?: str.toDoubleOrNull()?.toInt() ?: defaultValue
            }
            else -> defaultValue
        }
    }

    fun parseLongSafe(value: Any?, defaultValue: Long): Long {
        if (value == null) return defaultValue
        return when (value) {
            is Long -> value
            is Number -> value.toLong()
            is String -> {
                val str = value.trim()
                str.toLongOrNull() ?: str.toDoubleOrNull()?.toLong() ?: defaultValue
            }
            else -> defaultValue
        }
    }

    fun parseBooleanSafe(value: Any?, defaultValue: Boolean): Boolean {
        if (value == null) return defaultValue
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> {
                val str = value.trim().lowercase()
                str == "true" || str == "1" || str == "yes" || str == "on"
            }
            else -> defaultValue
        }
    }

    fun parseStringSafe(value: Any?, defaultValue: String): String {
        if (value == null) return defaultValue
        return when (value) {
            is String -> value
            else -> value.toString()
        }
    }

    /**
     * Migrate existing preferences to ensure their types match what the application expects.
     * This handles backwards compatibility with older versions where color values were stored as Strings
     * (e.g. from ListPreference) and prevents ClassCastException when reading them with getInt().
     */
    fun migratePreferences(prefs: SharedPreferences) {
        try {
            val all = prefs.all
            val editor = prefs.edit()
            var changed = false

            // Color preferences: must be stored as Int
            val colorKeys = listOf(
                "game_bar_title_color" to 0xFF9FCEDE.toInt(),
                "game_bar_value_color" to 0xFFFFB347.toInt(),
                "game_bar_background_color" to 0xFF0D1117.toInt()
            )

            for ((key, defaultColor) in colorKeys) {
                if (all.containsKey(key)) {
                    val raw = all[key]
                    if (raw !is Int) {
                        val parsed = parseColorSafe(raw, defaultColor)
                        editor.putInt(key, parsed)
                        changed = true
                        Log.i(TAG, "Migrated color preference $key from $raw (${raw?.javaClass?.simpleName}) to Int $parsed")
                    }
                }
            }

            // Dimension / Int preferences: must be stored as Int
            val intKeys = listOf(
                "game_bar_text_size" to 15,
                "game_bar_background_alpha" to 255,
                "game_bar_corner_radius" to 30,
                "game_bar_padding" to 4,
                "game_bar_item_spacing" to 8,
                "game_bar_overlay_width_dp" to 280
            )

            for ((key, defaultVal) in intKeys) {
                if (all.containsKey(key)) {
                    val raw = all[key]
                    if (raw !is Int) {
                        val parsed = parseIntSafe(raw, defaultVal)
                        editor.putInt(key, parsed)
                        changed = true
                        Log.i(TAG, "Migrated int preference $key from $raw (${raw?.javaClass?.simpleName}) to Int $parsed")
                    }
                }
            }

            // String preferences: must be stored as String
            val stringKeys = listOf(
                "game_bar_update_interval" to "1000",
                "game_bar_fps_display_mode" to "basic",
                "game_bar_fps_method" to "new",
                "game_bar_format" to "full",
                "game_bar_split_mode" to "side_by_side",
                "game_bar_single_tap_function" to "toggle_format",
                "game_bar_doubletap_function" to "toggle_fps_logging",
                "game_bar_longpress_function" to "load_preset",
                "game_bar_longpress_timeout" to "500",
                "game_bar_font_path" to "fonts/Circular-Std-Bold.ttf"
            )

            for ((key, defaultVal) in stringKeys) {
                if (all.containsKey(key)) {
                    val raw = all[key]
                    if (raw !is String) {
                        val parsed = parseStringSafe(raw, defaultVal)
                        editor.putString(key, parsed)
                        changed = true
                        Log.i(TAG, "Migrated string preference $key from $raw (${raw?.javaClass?.simpleName}) to String $parsed")
                    }
                }
            }

            // Boolean preferences: must be stored as Boolean
            val booleanKeys = listOf(
                "game_bar_enable" to false,
                "game_bar_auto_enable" to false,
                "game_bar_fps_enable" to true,
                "game_bar_frame_time_enable" to false,
                "game_bar_temp_enable" to false,
                "game_bar_cpu_usage_enable" to true,
                "game_bar_cpu_clock_enable" to false,
                "game_bar_cpu_temp_enable" to false,
                "game_bar_ram_enable" to false,
                "game_bar_ram_speed_enable" to false,
                "game_bar_ram_temp_enable" to false,
                "game_bar_gpu_usage_enable" to true,
                "game_bar_gpu_clock_enable" to false,
                "game_bar_gpu_temp_enable" to false,
                "game_bar_single_tap_enable" to true,
                "game_bar_doubletap_enable" to true,
                "game_bar_longpress_enable" to true,
                "game_bar_fps_record_control_enabled" to false
            )

            for ((key, defaultVal) in booleanKeys) {
                if (all.containsKey(key)) {
                    val raw = all[key]
                    if (raw !is Boolean) {
                        val parsed = parseBooleanSafe(raw, defaultVal)
                        editor.putBoolean(key, parsed)
                        changed = true
                        Log.i(TAG, "Migrated boolean preference $key from $raw (${raw?.javaClass?.simpleName}) to Boolean $parsed")
                    }
                }
            }

            // Legacy key migrations
            if (all.containsKey("game_bar_single_tap_toggle") && !all.containsKey("game_bar_single_tap_enable")) {
                val legacyVal = parseBooleanSafe(all["game_bar_single_tap_toggle"], true)
                editor.putBoolean("game_bar_single_tap_enable", legacyVal)
                changed = true
            }

            if (all.containsKey("game_bar_doubletap_capture") && !all.containsKey("game_bar_doubletap_enable")) {
                val legacyVal = parseBooleanSafe(all["game_bar_doubletap_capture"], true)
                editor.putBoolean("game_bar_doubletap_enable", legacyVal)
                changed = true
            }

            if (changed) {
                editor.apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during preference migration", e)
        }
    }
}

// Extension methods for SharedPreferences to safely read values regardless of underlying stored type
fun SharedPreferences.getSafeColor(key: String, defaultColor: Int): Int {
    return try {
        if (!contains(key)) return defaultColor
        val raw = all[key] ?: return defaultColor
        if (raw is Int) {
            raw
        } else {
            val parsed = GameBarPrefsUtils.parseColorSafe(raw, defaultColor)
            edit().putInt(key, parsed).apply()
            parsed
        }
    } catch (e: Exception) {
        defaultColor
    }
}

fun SharedPreferences.getSafeInt(key: String, defaultValue: Int): Int {
    return try {
        if (!contains(key)) return defaultValue
        val raw = all[key] ?: return defaultValue
        if (raw is Int) {
            raw
        } else {
            val parsed = GameBarPrefsUtils.parseIntSafe(raw, defaultValue)
            edit().putInt(key, parsed).apply()
            parsed
        }
    } catch (e: Exception) {
        defaultValue
    }
}

fun SharedPreferences.getSafeLong(key: String, defaultValue: Long): Long {
    return try {
        if (!contains(key)) return defaultValue
        val raw = all[key] ?: return defaultValue
        if (raw is Long) {
            raw
        } else {
            val parsed = GameBarPrefsUtils.parseLongSafe(raw, defaultValue)
            edit().putLong(key, parsed).apply()
            parsed
        }
    } catch (e: Exception) {
        defaultValue
    }
}

fun SharedPreferences.getSafeBoolean(key: String, defaultValue: Boolean): Boolean {
    return try {
        if (!contains(key)) return defaultValue
        val raw = all[key] ?: return defaultValue
        if (raw is Boolean) {
            raw
        } else {
            val parsed = GameBarPrefsUtils.parseBooleanSafe(raw, defaultValue)
            edit().putBoolean(key, parsed).apply()
            parsed
        }
    } catch (e: Exception) {
        defaultValue
    }
}

fun SharedPreferences.getSafeString(key: String, defaultValue: String): String {
    return try {
        if (!contains(key)) return defaultValue
        val raw = all[key] ?: return defaultValue
        if (raw is String) {
            raw
        } else {
            val parsed = GameBarPrefsUtils.parseStringSafe(raw, defaultValue)
            edit().putString(key, parsed).apply()
            parsed
        }
    } catch (e: Exception) {
        defaultValue
    }
}
