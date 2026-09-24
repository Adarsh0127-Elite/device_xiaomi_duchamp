/*
 * SPDX-FileCopyrightText: 2025-2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.gamebar

import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.Locale

object GameBarMemInfo {

    private val RAM_FREQ_FALLBACK_PATHS = arrayOf(
        "/sys/devices/platform/soc/1c00f000.dvfsrc/mtk-dvfsrc-devfreq/devfreq/mtk-dvfsrc-devfreq/cur_freq",
        "/sys/class/devfreq/mtk-dvfsrc-devfreq/cur_freq",
        "/sys/devices/system/cpu/bus_dcvs/DDR/cur_freq"
    )

    fun getRamUsage(): String {
        var memTotal = 0L
        var memAvailable = 0L

        try {
            BufferedReader(FileReader(GameBarConfig.procMeminfoPath)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    line?.let {
                        when {
                            it.startsWith("MemTotal:") -> memTotal = parseMemValue(it)
                            it.startsWith("MemAvailable:") -> memAvailable = parseMemValue(it)
                        }
                        if (memTotal > 0 && memAvailable > 0) {
                            return@use
                        }
                    }
                }
            }
        } catch (e: IOException) {
            return "N/A"
        }

        if (memTotal == 0L) {
            return "N/A"
        }

        val usedKb = memTotal - memAvailable
        val usedMb = usedKb / 1024
        return usedMb.toString()
    }

    private fun parseMemValue(line: String): Long {
        val parts = line.split("\\s+".toRegex())
        if (parts.size < 3) {
            return 0L
        }
        return try {
            parts[1].toLong()
        } catch (e: NumberFormatException) {
            0L
        }
    }

    fun getRamSpeed(): String {
        val candidatePaths = mutableListOf<String>()
        if (GameBarConfig.ramFreqPath.isNotEmpty()) {
            candidatePaths.add(GameBarConfig.ramFreqPath)
        }
        for (fallback in RAM_FREQ_FALLBACK_PATHS) {
            if (!candidatePaths.contains(fallback)) {
                candidatePaths.add(fallback)
            }
        }

        for (path in candidatePaths) {
            try {
                BufferedReader(FileReader(path)).use { br ->
                    val line = br.readLine()
                    if (!line.isNullOrEmpty()) {
                        val digits = line.trim().replace("[^0-9]".toRegex(), "")
                        if (digits.isNotEmpty()) {
                            val raw = digits.toLong()
                            if (raw > 0) {
                                val mhz = when {
                                    raw >= 100_000_000 -> raw / 1_000_000f
                                    raw >= 100_000 -> raw / 1_000f
                                    else -> raw.toFloat()
                                }
                                return if (mhz >= 1000f) {
                                    String.format(Locale.getDefault(), "%.3f GHz", mhz / 1000f)
                                } else {
                                    String.format(Locale.getDefault(), "%.0f MHz", mhz)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return "N/A"
    }

    fun getRamTemp(): String {
        try {
            val path = GameBarConfig.ramTempPath
            if (path.isNotEmpty() && path != "dynamic") {
                BufferedReader(FileReader(path)).use { br ->
                    val line = br.readLine()
                    if (!line.isNullOrEmpty()) {
                        try {
                            val raw = line.trim().toInt()
                            val divider = if (GameBarConfig.ramTempDivider > 0) GameBarConfig.ramTempDivider else 1000
                            val celsius = raw / divider.toFloat()
                            return String.format(Locale.getDefault(), "%.1f°C", celsius)
                        } catch (_: NumberFormatException) {}
                    }
                }
            }
        } catch (_: IOException) {}
        return "N/A"
    }

    fun getAppRamUsage(context: android.content.Context, packageName: String): String {
        try {
            val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val pids = am.runningAppProcesses?.filter { it.processName == packageName }?.map { it.pid }?.toIntArray()
            if (pids != null && pids.isNotEmpty()) {
                val memInfos = am.getProcessMemoryInfo(pids)
                var totalPssKb = 0
                for (info in memInfos) {
                    totalPssKb += info.totalPss
                }
                return (totalPssKb / 1024).toString()
            }
        } catch (_: Exception) {}
        return "N/A"
    }
}
