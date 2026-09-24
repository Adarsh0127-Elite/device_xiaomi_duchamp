/*
 * SPDX-FileCopyrightText: 2025-2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.gamebar

import com.xiaomi.settings.gamebar.utils.SysfsDetector
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.Locale

object GameBarGpuInfo {

    private val GPU_FREQ_FALLBACK_PATHS = arrayOf(
        "/sys/kernel/ged/hal/current_freqency",
        "/sys/kernel/ged/hal/previous_freqency",
        "/sys/class/devfreq/13000000.mali/cur_freq",
        "/sys/class/kgsl/kgsl-3d0/gpuclk"
    )

    private val GPU_USAGE_FALLBACK_PATHS = arrayOf(
        "/sys/kernel/ged/hal/gpu_utilization",
        "/sys/class/devfreq/13000000.mali/load",
        "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"
    )

    private val GPU_TEMP_FALLBACK_PATHS = arrayOf(
        "/sys/class/thermal/thermal_zone30/temp",
        "/sys/class/thermal/thermal_zone31/temp"
    )

    fun getGpuUsage(): String {
        var usage = parseGpuUsageFromPath(GameBarConfig.gpuUsagePath)
        if (usage != "N/A") return usage

        for (path in GPU_USAGE_FALLBACK_PATHS) {
            usage = parseGpuUsageFromPath(path)
            if (usage != "N/A") return usage
        }
        return "N/A"
    }

    private fun parseGpuUsageFromPath(path: String?): String {
        if (path.isNullOrEmpty()) return "N/A"
        val line = readLine(path)?.trim() ?: return "N/A"
        if (line.isEmpty()) return "N/A"

        return try {
            val parts = line.split("\\s+".toRegex())
            val token = parts.firstOrNull { it.matches(".*\\d+.*".toRegex()) } ?: line
            val digits = token.replace("[^0-9]".toRegex(), "")
            if (digits.isEmpty()) return "N/A"
            val value = digits.toInt()
            if (value in 0..100) value.toString() else "N/A"
        } catch (_: Exception) {
            "N/A"
        }
    }

    fun getGpuClock(): String {
        var freq = parseGpuClockFromPath(GameBarConfig.gpuClockPath, GameBarConfig.gpuClockDivider)
        if (freq != "N/A") return freq

        for (path in GPU_FREQ_FALLBACK_PATHS) {
            freq = parseGpuClockFromPath(path, 0)
            if (freq != "N/A") return freq
        }
        return "N/A"
    }

    private fun parseGpuClockFromPath(path: String?, configuredDivider: Int): String {
        if (path.isNullOrEmpty()) return "N/A"
        val line = readLine(path)?.trim() ?: return "N/A"
        if (line.isEmpty()) return "N/A"

        return try {
            // GED format: "40 740000" = OPP_IDX + KHz
            val parts = line.split("\\s+".toRegex())
            if (parts.size >= 2) {
                val khz = parts[1].replace("[^0-9]".toRegex(), "").toLongOrNull()
                if (khz != null && khz > 0) {
                    val mhz = khz / 1000
                    return mhz.toString()
                }
            }

            // Single number format (e.g. devfreq Hz or kgsl Hz/KHz/MHz)
            val raw = line.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: return "N/A"
            if (raw <= 0) return "0"

            val divider = when {
                configuredDivider > 0 -> configuredDivider
                raw >= 100_000_000 -> 1_000_000 // Hz -> MHz
                raw >= 100_000 -> 1_000 // KHz -> MHz
                else -> 1 // already MHz
            }
            val mhz = raw / divider
            mhz.toString()
        } catch (_: Exception) {
            "N/A"
        }
    }

    fun getGpuTemp(): String {
        val configTempPath = GameBarConfig.gpuTempPath
        if (!configTempPath.isNullOrEmpty() && configTempPath != "dynamic") {
            val line = readLine(configTempPath)
            if (line != null) {
                try {
                    val raw = line.trim().toFloat()
                    val divider = if (GameBarConfig.gpuTempDivider > 0) GameBarConfig.gpuTempDivider else SysfsDetector.detectTemperatureDivider(configTempPath)
                    val celsius = raw / divider.toFloat()
                    if (celsius in 0f..120f) {
                        return String.format(Locale.getDefault(), "%.1f", celsius)
                    }
                } catch (_: Exception) {}
            }
        }

        // Try reading duchamp specific GPU thermal zones (thermal_zone30 & thermal_zone31)
        var total = 0f
        var count = 0
        for (path in GPU_TEMP_FALLBACK_PATHS) {
            val line = readLine(path) ?: continue
            try {
                val raw = line.trim().toFloat()
                if (raw <= 0 || raw > 120000) continue
                val temp = if (raw > 1000) raw / 1000f else raw
                if (temp in 0f..120f) {
                    total += temp
                    count++
                }
            } catch (_: Exception) {}
        }
        if (count > 0) {
            return String.format(Locale.getDefault(), "%.1f", total / count)
        }

        return "N/A"
    }

    private fun readLine(path: String): String? {
        return try {
            BufferedReader(FileReader(path)).use { it.readLine() }
        } catch (_: IOException) {
            null
        }
    }
}
