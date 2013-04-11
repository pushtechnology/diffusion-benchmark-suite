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
