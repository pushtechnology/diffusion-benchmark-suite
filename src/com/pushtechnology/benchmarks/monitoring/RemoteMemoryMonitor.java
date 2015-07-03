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

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;

/**
 * Remote memory monitoring utility.
 * 
 * @author nitsanw
 * 
 */
public final class RemoteMemoryMonitor implements MemoryMonitor {
    // CHECKSTYLE:OFF
    private final MBeanServerConnection connection;
    private final ObjectName osBeanName;
    private CompositeDataSupport currentMemoryUsage;
    private CompositeDataSupport currentOffHeapMemoryUsage;

    private boolean circuitBroken = false;
    
    /**
     * Constructor for remote memory monitor.
     * 
     * @param connection to remote process
     * @throws MalformedObjectNameException if bean is not found
     */
    public RemoteMemoryMonitor(MBeanServerConnection connection)
            throws MalformedObjectNameException {
        // CHECKSTYLE:ON
        this.connection = connection;
        this.osBeanName = new ObjectName("java.lang:type=Memory");
    }

    @Override
    public long heapCommitted() {
        if (getMemoryUsageData() == null) {
            return -1;
        }
        return ((Long) getMemoryUsageData().get("committed"));
    }

    @Override
    public long heapUsed() {
        if (getMemoryUsageData() == null) {
            return -1;
        }
        return (Long) getMemoryUsageData().get("used");
    }

    @Override
    public long heapMax() {
        if (getMemoryUsageData() == null) {
            return -1;
        }
        return (Long) getMemoryUsageData().get("max");
    }

    @Override
    public long offHeapCommitted() {
        if (getOffHeapMemoryUsageData() == null) {
            return -1;
        }
        return (Long) getOffHeapMemoryUsageData().get("committed");
    }

    @Override
    public long offHeapUsed() {
        if (getOffHeapMemoryUsageData() == null) {
            return -1;
        }
        return (Long) getOffHeapMemoryUsageData().get("used");
    }

    @Override
    public long offHeapMax() {
        if (getOffHeapMemoryUsageData() == null) {
            return -1;
        }
        return (Long) getOffHeapMemoryUsageData().get("max");
    }

    /**
     * @return current heap memory use
     */
    private CompositeDataSupport getMemoryUsageData() {
        try {
            return currentMemoryUsage;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @return current heap memory use
     */
    private CompositeDataSupport getOffHeapMemoryUsageData() {
        try {
            return currentOffHeapMemoryUsage;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * If the jmx sampling takes too long, or if there is
     * an error, then disable/ break the circuit on this
     * remote call.
     */
    @Override
    public void sample() {
    	
    	if(!circuitBroken){
    		long start = System.currentTimeMillis();
    		long end=-1;
        try {
            currentMemoryUsage =
                    (CompositeDataSupport) connection.getAttribute(osBeanName,
                            "HeapMemoryUsage");
            currentOffHeapMemoryUsage =
                    (CompositeDataSupport) connection.getAttribute(osBeanName,
                            "NonHeapMemoryUsage");
        } catch (Exception e) {
            currentMemoryUsage = null;
            currentOffHeapMemoryUsage = null;
        } finally {
    		end = System.currentTimeMillis();
        }
        if(end-start > 400){
            currentMemoryUsage = null;
            currentOffHeapMemoryUsage = null;
            breakCircuit();
        }
    	}
    }
    
    public void breakCircuit(){
    	circuitBroken=true;
    }
}
