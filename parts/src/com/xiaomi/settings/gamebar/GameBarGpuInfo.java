/*
 * Copyright (C) 2025 kenway214
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaomi.settings.gamebar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;
import android.util.Log;

public class GameBarGpuInfo {

    private static final String[] GPU_TEMP_PATHS = {
            "/sys/class/thermal/thermal_zone30/temp",
            "/sys/class/thermal/thermal_zone31/temp"
    };

    // Set GPU Mali
    private static final String[] GPU_FREQ_PATHS = {
            "/sys/kernel/ged/hal/current_freqency",
            "/sys/kernel/ged/hal/previous_freqency"
    };

    public static String getGpuTemp() {
        float total = 0f;
        int count = 0;

        for (String path : GPU_TEMP_PATHS) {
            String line = readLine(path);
            if (line == null) continue;
            try {
                float raw = Float.parseFloat(line.trim());
                if (raw <= 0 || raw > 120000) continue;

                float temp = raw / 1000f;
                total += temp;
                count++;
            } catch (Exception ignored) {}
        }

        return count > 0 ? String.format("%.1f", total / count) : "N/A";
    }

    public static String getGpuFreq() {
        String line = readLine("/sys/kernel/ged/hal/current_freqency");

        if (line == null || line.trim().isEmpty()) {
            return "N/A";
        }

        try {
            String[] parts = line.trim().split("\\s+");

            if (parts.length >= 2) {
                long khz = Long.parseLong(parts[1]);
                return (khz / 1000) + " MHz";
            }
        } catch (Exception ignored) {
        }

        return "N/A";
    }

    private static String parseGpuFreq(String line) {
        if (line == null) return "N/A";

        try {
            line = line.trim();

            // GED format: "40 740000" = OPP_IDX + KHz
            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                long khz = Long.parseLong(parts[1].replaceAll("[^0-9]", ""));
                if (khz > 0) {
                    return (khz / 1000) + " MHz";
                }
            }

            // Devfreq format: "740000000" = Hz
            long hz = Long.parseLong(line.replaceAll("[^0-9]", ""));
            if (hz > 0) {
                return (hz / 1000000) + " MHz";
            }

            return "0 MHz";
        } catch (Exception ignored) {
            return "N/A";
        }
    }

    private static String readLine(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return br.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
