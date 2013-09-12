package com.pushtechnology.benchmarks.monitoring;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * A remote process cpu monitor using a JMX connection. Relies on the
 * availability of the java.lang:type=OperatingSystem bean and it's 
 * ProcessCpuTime attribute.
 * 
 * @author nitsanw
 *
 */
public class RemoteCpuMonitor extends CpuMonitor {
    // CHECKSTYLE:OFF
    private final MBeanServerConnection connection;
    private final ObjectName osBeanName;
    /**
     * @param connection to remote process
     * @throws MalformedObjectNameException if bean is not found
     */
    public RemoteCpuMonitor(MBeanServerConnection connection)
            throws MalformedObjectNameException {
        // CHECKSTYLE:ON
        this.connection = connection;
        this.osBeanName = new ObjectName("java.lang:type=OperatingSystem");
    }

    @Override
    protected final long getProcessCpuTime() {
        try {
            return (Long) connection.getAttribute(osBeanName, "ProcessCpuTime");
        } catch (Exception e) {
            return 0;
        }
    }

}
