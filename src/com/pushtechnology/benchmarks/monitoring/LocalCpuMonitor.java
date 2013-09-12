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
 * Local CPU monitor.
 * 
 * @author nitsanw
 *
 */
public class LocalCpuMonitor extends CpuMonitor {

    /** .. */
    private final OperatingSystemMXBean osMBean = ManagementFactory
                .getOperatingSystemMXBean();

    @Override
    protected final long getProcessCpuTime() {
        long processCpuTime = 0;
    
        if (osMBean instanceof com.sun.management.OperatingSystemMXBean) {
            processCpuTime = ((com.sun.management.OperatingSystemMXBean)
                    osMBean).getProcessCpuTime();
        }
        return processCpuTime;
    }

}
