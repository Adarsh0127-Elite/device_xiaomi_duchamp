/*
 * SPDX-FileCopyrightText: 2025-2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.gamebar

import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.io.File
import java.util.*

object GameBarCpuInfo {

    private var prevIdle = -1L
    private var prevTotal = -1L

    private val CPU_TEMP_FALLBACK_PATHS = Array(16) { i ->
        "/sys/class/thermal/thermal_zone${10 + i}/temp"
    }

    fun getCpuUsage(): String {
        val line = readLine(GameBarConfig.procStatPath)
        if (line == null || !line.startsWith("cpu ")) return "N/A"
        val parts = line.split("\\s+".toRegex())
        if (parts.size < 8) return "N/A"

        return try {
            val user = parts[1].toLong()
            val nice = parts[2].toLong()
            val system = parts[3].toLong()
            val idle = parts[4].toLong()
            val iowait = parts[5].toLong()
            val irq = parts[6].toLong()
            val softirq = parts[7].toLong()
            val steal = if (parts.size > 8) parts[8].toLong() else 0L

            val total = user + nice + system + idle + iowait + irq + softirq + steal

            if (prevTotal != -1L && total != prevTotal) {
                val diffTotal = total - prevTotal
                val diffIdle = idle - prevIdle
                val usage = 100 * (diffTotal - diffIdle) / diffTotal
                prevTotal = total
                prevIdle = idle
                usage.toString()
            } else {
                prevTotal = total
                prevIdle = idle
                "N/A"
            }
        } catch (e: NumberFormatException) {
            "N/A"
        }
    }

    fun getCpuFrequencies(): List<String> {
        val result = mutableListOf<String>()
        val basePath = GameBarConfig.cpuBasePath ?: return result

        val cpuDir = File(basePath)
        val files = cpuDir.listFiles { _, name -> name.matches(Regex("cpu\\d+")) }
        if (files.isNullOrEmpty()) {
            return result
        }

        val cpuFolders = files.toMutableList()
        cpuFolders.sortBy { extractCpuNumber(it) }

        for (cpu in cpuFolders) {
            val freqPath = "${cpu.absolutePath}/cpufreq/scaling_cur_freq"
            val freqStr = readLine(freqPath)
            if (!freqStr.isNullOrEmpty()) {
                try {
                    val khz = freqStr.trim().toInt()
                    val mhz = khz / 1000
                    result.add("${cpu.name}: $mhz MHz")
                } catch (e: NumberFormatException) {
                    result.add("${cpu.name}: N/A")
                }
            } else {
                result.add("${cpu.name}: offline or frequency not available")
            }
        }
        return result
    }

    fun getCpuTemp(): String {
        val (path, divider) = GameBarConfig.getCpuTempConfig()
        if (path != null) {
            val line = readLine(path)
            if (line != null) {
                try {
                    val raw = line.trim().toFloat()
                    val celsius = raw / divider.toFloat()
                    if (celsius in 0f..150f) {
                        return String.format(Locale.getDefault(), "%.1f", celsius)
                    }
                } catch (_: NumberFormatException) {}
            }
        }

        // Fallback to duchamp MT6897 CPU thermal zones 10-25
        var total = 0f
        var count = 0
        for (fallbackPath in CPU_TEMP_FALLBACK_PATHS) {
            val line = readLine(fallbackPath) ?: continue
            try {
                val raw = line.trim().toFloat()
                if (raw <= 0 || raw > 150000) continue
                val temp = if (raw > 1000) raw / 1000f else raw
                if (temp in 0f..150f) {
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

    private fun extractCpuNumber(cpuFolder: File): Int {
        val name = cpuFolder.name.replace("cpu", "")
        return try {
            name.toInt()
        } catch (e: NumberFormatException) {
            -1
        }
    }

    private fun readLine(path: String): String? {
        return try {
            BufferedReader(FileReader(path)).use { it.readLine() }
        } catch (e: IOException) {
            null
        }
    }
}
