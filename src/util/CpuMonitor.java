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
package util;

import java.lang.management.ManagementFactory;

import java.lang.management.OperatingSystemMXBean;
public class CpuMonitor { 
    private long lastSystemTime      = 0;
    private long lastProcessCpuTime  = 0;
    private OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();

    public synchronized double getCpuUsage()
    {
        if ( lastSystemTime == 0 )
        {
            baselineCounters();
            return 0.0;
        }

        long systemTime     = System.nanoTime();
        long processCpuTime = 0;

        if ( getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean )
        {
            processCpuTime = ( (com.sun.management.OperatingSystemMXBean) getOperatingSystemMXBean() ).getProcessCpuTime();
        }

        double cpuUsage = (double) ( processCpuTime - lastProcessCpuTime )*100.0 / ( systemTime - lastSystemTime );

        lastSystemTime     = systemTime;
        lastProcessCpuTime = processCpuTime;

        return cpuUsage;
    }

    private OperatingSystemMXBean getOperatingSystemMXBean() {
        return bean ;
    }

    private void baselineCounters()
    {
        lastSystemTime = System.nanoTime();

        if ( getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean )
        {
            lastProcessCpuTime = ( (com.sun.management.OperatingSystemMXBean) getOperatingSystemMXBean() ).getProcessCpuTime();
        }
    }
}
