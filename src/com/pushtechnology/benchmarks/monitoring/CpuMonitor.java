/*
 * Copyright 2013 Push Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.pushtechnology.benchmarks.monitoring;

import java.lang.management.ManagementFactory;

import java.lang.management.OperatingSystemMXBean;

/**
 * A utility for monitoring CPU usage. Only works on the Oracle/Sun JVM.
 * 
 * @author nitsanw
 * 
 */
public final class CpuMonitor {
    // CHECKSTYLE:OFF
    private long lastSampleSystemTime = 0;
    private long lastSampleProcessCpuTime = 0;
    private OperatingSystemMXBean bean = ManagementFactory
            .getOperatingSystemMXBean();
    // CHECKSTYLE:ON
    /**
     * @return cpu usage in percent of logical cpu
     */
    public double getCpuUsage() {
        if (lastSampleSystemTime == 0) {
            lastSampleSystemTime = System.nanoTime();

            if (bean instanceof com.sun.management.OperatingSystemMXBean) {
                lastSampleProcessCpuTime =
                        ((com.sun.management.OperatingSystemMXBean) bean)
                                .getProcessCpuTime();
            }
            return 0.0;
        }

        long systemTime = System.nanoTime();
        long processCpuTime = 0;

        if (bean instanceof com.sun.management.OperatingSystemMXBean) {
            processCpuTime = ((com.sun.management.OperatingSystemMXBean)
                    bean)
                            .getProcessCpuTime();
        }
        // CHECKSTYLE:OFF
        double cpuUsage = (double) (processCpuTime - lastSampleProcessCpuTime)
                * 100.0 / (systemTime - lastSampleSystemTime);
        // CHECKSTYLE:ON
        lastSampleSystemTime = systemTime;
        lastSampleProcessCpuTime = processCpuTime;

        return cpuUsage;
    }
}
