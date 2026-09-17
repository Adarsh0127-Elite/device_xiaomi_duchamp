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

public class GameBarGpuInfo {

    private static final String[] GPU_TEMP_PATHS = {
        "/sys/class/thermal/thermal_zone30/temp",
        "/sys/class/thermal/thermal_zone31/temp"
    };

    // Set GPU Mali
    private static final String GPU_FREQ_PATH =
        "/sys/class/devfreq/13000000.mali/cur_freq";

    // =========================
    //GPU TEMP (SoC-based)
    // =========================
    public static String getGpuTemp() {
        float total = 0f;
        int count = 0;

        for (String path : GPU_TEMP_PATHS) {
            String line = readLine(path);
            if (line == null) continue;

            try {
                float raw = Float.parseFloat(line.trim());

                // Filter out invalid values
                if (raw <= 0 || raw > 120000) continue;

                float temp = raw / 1000f;

                total += temp;
                count++;

            } catch (Exception ignored) {}
        }

        return count > 0 ? String.format("%.1f", total / count) : "N/A";
    }

    // =========================
    // ⚡ GPU FREQ (MHz)
    // =========================
    public static String getGpuFreq() {
        String line = readLine(GPU_FREQ_PATH);
        if (line == null) return "N/A";

        try {
            long hz = Long.parseLong(line.trim());

            if (hz <= 0) return "0 MHz";

            long mhz = hz / 1000000; // Hz → MHz
            return mhz + " MHz";

        } catch (Exception e) {
            return "N/A";
        }
    }

    // =========================
    //  Read helper
    // =========================
    private static String readLine(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return br.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
