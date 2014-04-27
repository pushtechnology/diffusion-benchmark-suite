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

/**
 * A utility for monitoring CPU usage. Only works on the Oracle/Sun JVM.
 * 
 * @author nitsanw
 * 
 */
public abstract class CpuMonitor {
    // CHECKSTYLE:OFF
    private long lastSampleSystemTime = 0;
    private long lastSampleProcessCpuTime = 0;
    // CHECKSTYLE:ON
    /**
     * @return cpu usage in percent of logical cpu
     */
    public final double getCpuUsage() {
        if (lastSampleSystemTime == 0) {
            lastSampleSystemTime = System.nanoTime();
            lastSampleProcessCpuTime = getProcessCpuTime();
            return 0.0;
        }

        long systemTime = System.nanoTime();
        long processCpuTime = getProcessCpuTime();
        // CHECKSTYLE:OFF
        long dCpu = processCpuTime - lastSampleProcessCpuTime;
        long dTime = systemTime - lastSampleSystemTime;
        double cpuUsage = (double) (dCpu * 100.0) / dTime;
        // CHECKSTYLE:ON
        lastSampleSystemTime = systemTime;
        lastSampleProcessCpuTime = processCpuTime;

        return Math.max(cpuUsage, 0.0);
    }

    /**
     * @return cpu time for the process in nanos
     */
    protected abstract long getProcessCpuTime();
}
