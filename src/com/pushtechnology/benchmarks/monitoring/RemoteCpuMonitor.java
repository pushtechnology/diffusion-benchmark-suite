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
	private boolean circuitBroken=false;
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

    /**
     * If the jmx sampling takes too long, or if there is
     * an error, then disable/ break the circuit on this
     * remote call.
     */
    @Override
    protected final long getProcessCpuTime() {

    	if(!circuitBroken){
    		long start = System.currentTimeMillis();
    		long end=-1;
        try {
            return (Long) connection.getAttribute(osBeanName, "ProcessCpuTime");
        } catch (Exception e) {
        	//circuitBroken=true;
            return 0;
        } finally {
    		end = System.currentTimeMillis();
        if(end-start > 900){
            breakCircuit();
        }
        }
    	} else {
    		return 0;
    	}
    }

    public void breakCircuit(){
    	circuitBroken=true;
    }

}
