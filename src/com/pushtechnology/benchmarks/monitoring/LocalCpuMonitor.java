package com.pushtechnology.benchmarks.monitoring;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class LocalCpuMonitor extends CpuMonitor {

    private OperatingSystemMXBean bean = ManagementFactory
                .getOperatingSystemMXBean();

    protected long getProcessCpuTime() {
        long processCpuTime = 0;
    
        if (bean instanceof com.sun.management.OperatingSystemMXBean) {
            processCpuTime = ((com.sun.management.OperatingSystemMXBean)
                    bean).getProcessCpuTime();
        }
        return processCpuTime;
    }

}
