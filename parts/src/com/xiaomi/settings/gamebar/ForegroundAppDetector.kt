/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */


package com.xiaomi.settings.gamebar

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.lang.reflect.Method

object ForegroundAppDetector {

    private const val TAG = "ForegroundAppDetector"
    private var lastKnownPackage = "Unknown"
    private var lastUpdateTime = 0L
    private const val CACHE_TIMEOUT = 500L // Reduce cache timeout
    
    // Simple reflection caching
    private var reflectionSetupFailed = false

    fun getForegroundPackageName(context: Context): String {
        // Use cached result if still valid
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime < CACHE_TIMEOUT && lastKnownPackage != "Unknown") {
            return lastKnownPackage
        }

        val atmPkg = tryReflectActivityTaskManager()
        if (!atmPkg.isNullOrEmpty() && atmPkg != "Unknown") {
            if (isTransientSystemApp(atmPkg)) {
                if (lastKnownPackage != "Unknown") {
                    return lastKnownPackage
                }
            } else {
                lastKnownPackage = atmPkg
                lastUpdateTime = currentTime
                return atmPkg
            }
        }

        val pkg = tryGetRunningTasks(context)
        if (!pkg.isNullOrEmpty() && pkg != "Unknown") {
            if (isTransientSystemApp(pkg)) {
                if (lastKnownPackage != "Unknown") {
                    return lastKnownPackage
                }
            } else {
                lastKnownPackage = pkg
                lastUpdateTime = currentTime
                return pkg
            }
        }

        val usagePkg = tryGetUsageStats(context)
        if (!usagePkg.isNullOrEmpty() && usagePkg != "Unknown") {
            if (isTransientSystemApp(usagePkg)) {
                if (lastKnownPackage != "Unknown") {
                    return lastKnownPackage
                }
            } else {
                lastKnownPackage = usagePkg
                lastUpdateTime = currentTime
                return usagePkg
            }
        }

        // Return cached value if available, otherwise "Unknown"
        return lastKnownPackage
    }

    private fun isTransientSystemApp(pkg: String): Boolean {
        return pkg == "com.xiaomi.settings" || pkg == "com.android.systemui" || pkg == "android"
    }

    private fun tryGetRunningTasks(context: Context): String? {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val tasks = am?.getRunningTasks(1)
            if (!tasks.isNullOrEmpty()) {
                tasks[0].topActivity?.packageName
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "tryGetRunningTasks error: ${e.message}")
            null
        }
    }

    private fun tryReflectActivityTaskManager(): String? {
        return try {
            val atmClass = Class.forName("android.app.ActivityTaskManager")
            val getServiceMethod = atmClass.getDeclaredMethod("getService")
            getServiceMethod.isAccessible = true
            val atmService = getServiceMethod.invoke(null) ?: return null

            // 1. Try getFocusedRootTaskInfo
            try {
                val getFocusedMethod = atmService.javaClass.getMethod("getFocusedRootTaskInfo")
                val rootTask = getFocusedMethod.invoke(atmService)
                if (rootTask != null) {
                    val rtiClass = rootTask.javaClass
                    val topActivityField = try {
                        rtiClass.getDeclaredField("topActivity").apply { isAccessible = true }.get(rootTask)
                    } catch (_: Exception) {
                        rtiClass.getMethod("getTopActivity").invoke(rootTask)
                    }
                    if (topActivityField != null) {
                        val getPackageNameMethod = topActivityField.javaClass.getMethod("getPackageName")
                        val pkgName = getPackageNameMethod.invoke(topActivityField) as? String
                        if (!pkgName.isNullOrEmpty()) {
                            return pkgName
                        }
                    }
                }
            } catch (_: Exception) {}

            // 2. Try getTasks(1)
            val getTasksMethod = atmService.javaClass.getMethod("getTasks", Int::class.java)
            @Suppress("UNCHECKED_CAST")
            val taskList = getTasksMethod.invoke(atmService, 1) as? List<*>
            if (!taskList.isNullOrEmpty()) {
                val firstTask = taskList[0] ?: return null
                val rtiClass = firstTask.javaClass
                val getTopActivityMethod = rtiClass.getDeclaredMethod("getTopActivity")
                val compName = getTopActivityMethod.invoke(firstTask)
                if (compName != null) {
                    val getPackageNameMethod = compName.javaClass.getMethod("getPackageName")
                    return getPackageNameMethod.invoke(compName) as? String
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "tryReflectActivityTaskManager error: ${e.message}")
            null
        }
    }

    private fun tryGetUsageStats(context: Context): String? {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
            if (usm == null) return null
            val time = System.currentTimeMillis()
            val events = usm.queryEvents(time - 15000, time)
            val event = android.app.usage.UsageEvents.Event()
            var latestPackage: String? = null
            var latestTime = 0L
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED &&
                    event.timeStamp >= latestTime
                ) {
                    latestPackage = event.packageName
                    latestTime = event.timeStamp
                }
            }
            latestPackage
        } catch (e: Exception) {
            Log.w(TAG, "tryGetUsageStats error: ${e.message}")
            null
        }
    }
}
