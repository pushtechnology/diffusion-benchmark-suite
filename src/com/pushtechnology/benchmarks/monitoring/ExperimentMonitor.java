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

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.benchmarks.experiments.CommonExperimentSettings;
import com.pushtechnology.benchmarks.util.JmxHelper;
import com.pushtechnology.benchmarks.util.Memory;
 
/**
 * This is a background thread for monitoring an experiment and output the
 * experiment metrics every second.
 * 
 * @author nitsanw
 */
public class ExperimentMonitor implements Runnable {
    // CHECKSTYLE:OFF
    private static final int MILLIS_IN_SECOND = 1000;
    private final ExperimentCounters experimentCounters;
    private final CpuMonitor cpuMonitor = System.getProperty("java.vendor")
            .startsWith("Oracle") ? new LocalCpuMonitor() : null;
    private MemoryMonitor memoryMonitor;
    private CpuMonitor rCpuMonitor;
    private MemoryMonitor rMemoryMonitor;
    private PrintStream out;

    private volatile boolean isRunning = true;
    private volatile boolean isSampling = false;
    private Thread monitorThread;
    private volatile long deadline;
    
    private final String RMI_TIMEOUT_MS = "80";
	private final CommonExperimentSettings settings;

    private static final Logger LOG = LoggerFactory.getLogger(ExperimentMonitor.class);

    /**
     * Create monitor for experiment.
     * 
     * @param experimentCountersP Counters
     * @param outputFilename Name of output file
     * @param host Name of server host
     */
    public ExperimentMonitor(final ExperimentCounters experimentCountersP,
    		final CommonExperimentSettings settings){
        this.experimentCounters = experimentCountersP;
        this.settings = settings;
        
        setupJmx();
        setupPrintstream();
    }

    @SuppressWarnings("resource")
    private void setupPrintstream() {
        if (settings.getOutputFile() == null || settings.getOutputFile().isEmpty()) {
            out = System.out;
        } else {
            PrintStream o;
            try {
                o = new PrintStream(settings.getOutputFile());
            } catch (FileNotFoundException e) {
                LOG.warn("failed to create output file: "
                        + settings.getOutputFile() + " will use sysout instead.");
                o = System.out;
            }
            out = o;
        }
	}

	private void setupJmx() {
        try {
        	// set the jmx timeout so the monitor loop doesn't get blocked
        	// http://docs.oracle.com/javase/7/docs/technotes/guides/rmi/sunrmiproperties.html
        	System.setProperty("sun.rmi.transport.tcp.responseTimeout", RMI_TIMEOUT_MS);
        	
            final JMXConnector connect =
                    JmxHelper.getJmxConnector(settings.getDiffusionHost(), "jmxrmi", "guest",
                            "Guest");
            final MBeanServerConnection mBeanServerConnection =
                    connect.getMBeanServerConnection();

            rMemoryMonitor = new RemoteMemoryMonitor(mBeanServerConnection);
            rCpuMonitor = new RemoteCpuMonitor(mBeanServerConnection);
            
        } catch (Exception e) {
            LOG.warn("Unable to create JMX connection to server - no further JMX monitoring possible", e);
        }
        
        try{
        	memoryMonitor = new LocalMemoryMonitor();
        } catch (Exception e) {
            LOG.warn("Unable to create JMX connection to test client for LocalMemoryMonitor", e);
        }
	}

	// CHECKSTYLE:ON
    @Override
    public final void run() {

        printHeader();
        monitorLoop();
        printFooter();
    }

    /**
     * Some attempt so far
     * is made to cater for times when
     * the JMX monitoring is slow to respond. Ideally
     * the loop should not be blocked.
     * 
     * @param timeStart
     */
    private void monitorLoop() {
    	
        long messagesBefore = experimentCounters.getMessageCounter();
        long bytesBefore = experimentCounters.getBytesCounter();
        long timeBefore = System.nanoTime();
        deadline = System.currentTimeMillis();
        long timeStart = System.nanoTime();
        
        while (isRunning) {
            final long timeAfter = System.nanoTime();
            
            final long messagesAfter = experimentCounters.getMessageCounter();
            final long bytesAfter = experimentCounters.getBytesCounter();
            final long intervalMessages = messagesAfter - messagesBefore;
            final long intervalBytes = bytesAfter - bytesBefore;
            final long intervalNanos = timeAfter - timeBefore;
            messagesBefore = messagesAfter;
            bytesBefore = bytesAfter;
            timeBefore = timeAfter;

            final long messagesPerSecond = (long) intervalMessages
                    * TimeUnit.SECONDS.toNanos(1) / intervalNanos;
            final long bytesPerSecond = (long) intervalBytes
                    * TimeUnit.SECONDS.toNanos(1) / intervalNanos;

            // capture local counters first
            long timestamp = (timeAfter - timeStart)/1000000;
            long currentlyConnected = experimentCounters.getCurrentlyConnected();
            long topicsCounter = experimentCounters.getTopicsCounter();
            long disconnects = experimentCounters.getClientDisconnectCounter();
            long refuseds = experimentCounters.getConnectionRefusedCounter();
            long connectAttempts = experimentCounters.getConnectionAttemptsCounter();
            
            String cpu = getCpu();
            memoryMonitor.sample();
            
            getOutput().format("%s, %d, %d, %d, %b, %s, %d, %d, %d, %d, %s, "
                    + "%s, %s, %s, %s, %s",
                    timestamp,
                    messagesPerSecond,
                    currentlyConnected,
                    topicsCounter,
                    isSampling,
                    cpu,
                    disconnects,
                    refuseds,
                    connectAttempts,
                    bytesPerSecond,
                    Memory.formatMemory(memoryMonitor.heapUsed()),
                    Memory.formatMemory(memoryMonitor.heapCommitted()),
                    Memory.formatMemory(memoryMonitor.heapMax()),
                    Memory.formatMemory(memoryMonitor.offHeapUsed()),
                    Memory.formatMemory(memoryMonitor.offHeapCommitted()),
                    Memory.formatMemory(memoryMonitor.offHeapMax()));

            // JMX counters - remote so could delay/slow
            if (rMemoryMonitor != null) {
                rMemoryMonitor.sample();
            }
            getOutput().format(", %s, %s, %s, %s, %s, %s, %s , %d, %d\n",
                    getServerCpu(),
                    getServerHeapUsed(),
                    getServerHeapCommitted(),
                    getServerHeapMax(),
                    getServerOffHeapUsed(),
                    getServerOffHeapCommitted(),
                    getServerOffHeapMax(),
                    experimentCounters.getAverageClientQueueSize(),
                    experimentCounters.getAverageClientQueueSizeHighWatermark());
            
            if (isSampling) {
            	experimentCounters.getMessageThroughputHistogram().recordValue(messagesPerSecond);
            } else {
            	experimentCounters.getMessageThroughputHistogram().reset();
            }
            // Single writer to this counter, so lazy set is fine
            experimentCounters.setLastMessagesPerSecond(messagesPerSecond);
            
            // warning for reporting delays
            long now = System.currentTimeMillis();
            long difference = deadline - now;
            if(difference < -200){
            	LOG.error("Monitoring Event loop is delayed by over 200ms. Data may be lost or inaccurate.");
            }
            if(difference < -1000){
                // we have missed one or more intervals, 
                // so update to the next interval deadline in the future 
                // else we get several intervals with very short duration
            	LOG.error("Monitoring Event loop - skipping interval(s).");
            	deadline += ((difference -  difference% MILLIS_IN_SECOND)/MILLIS_IN_SECOND+1)*MILLIS_IN_SECOND;
            } else {
                deadline += MILLIS_IN_SECOND;
            }
            
            LockSupport.parkUntil(deadline);
        }
		
	}

	private void printHeader() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
       long currentTimeMillis = System.currentTimeMillis();
       long timeStartNanos = System.nanoTime();
       
        getOutput().println("# Start timestamps : "+format.format(currentTimeMillis)+" currentTimeMillis: "+currentTimeMillis+" nanoTime: "+timeStartNanos);
        getOutput().println("ElapsedTimeMS, MessagesPerSecond, ClientsConnected, Topics,"
                + " InSample, Cpu, ClientDisconnects, ConnectionRefusals, "
                + "ConnectionAttempts, BytesPerSecond, UsedHeapMB, "
                + "CommitedHeapMB, MaxHeapMB, UsedOffHeapMB, CommittedOffHeapMB"
                + ", MaxOffHeapMB, ServerCPU, ServerHeapUsedMB, "
                + "ServerHeapCommittedMB, ServerMaxHeapMB, ServerOffHeapUsedMB,"
                + " ServerOffHeapCommittedMB, ServerOffHeapMaxMB,"
                + " AvgClientQueueSz, AvgClientQueueHwm");
		
	}

	private void printFooter() {
        getOutput().println("-------");
        getOutput().print("Throughput [count: ");
        getOutput().print(experimentCounters.getMessageThroughputHistogram().getTotalCount());
        getOutput().print(" max:");
        getOutput().print(experimentCounters.getMessageThroughputHistogram().getMaxValue());
        getOutput().print(" avg:");
        getOutput().print(experimentCounters.getMessageThroughputHistogram().getMean());
        getOutput().print(" mid:");
        // CHECKSTYLE:OFF
        getOutput().print(experimentCounters.getMessageThroughputHistogram().getValueAtPercentile(50));
        // CHECKSTYLE:ON

        getOutput().print(" min:");
        getOutput().print(experimentCounters.getMessageThroughputHistogram().getMinValue());
        getOutput().println("]");
		
	}

	/**
     * @return server max heap formatted, or N/A if no monitor exists
     */
    private String getServerHeapMax() {
        if (rMemoryMonitor == null) {
            return "N/A";
        }
        return Memory.formatMemory(rMemoryMonitor.heapMax());
    }

    /**
     * @return server used heap formatted, or N/A if no monitor exists
     */
    private String getServerHeapUsed() {
        if (rMemoryMonitor == null) {
            return "N/A";
        }
        return Memory.formatMemory(rMemoryMonitor.heapUsed());
    }

    /**
     * @return server committed heap formatted, or N/A if no monitor exists
     */
    private String getServerHeapCommitted() {
        if (rMemoryMonitor == null) {
            return "N/A";
        }
        return Memory.formatMemory(rMemoryMonitor.heapCommitted());
    }

    /**
     * @return server max off heap formatted, or N/A if no monitor exists
     */
    private String getServerOffHeapMax() {
        if (rMemoryMonitor == null) {
            return "N/A";
        }
        return Memory.formatMemory(rMemoryMonitor.offHeapMax());
    }

    /**
     * @return server used off heap formatted, or N/A if no monitor exists
     */
    private String getServerOffHeapUsed() {
        if (rMemoryMonitor == null) {
            return "N/A";
        }
        return Memory.formatMemory(rMemoryMonitor.offHeapUsed());
    }

    /**
     * @return server committed off heap formatted, or N/A if no monitor exists
     */
    private String getServerOffHeapCommitted() {
        if (rMemoryMonitor == null) {
            return "N/A";
        }
        return Memory.formatMemory(rMemoryMonitor.offHeapCommitted());
    }

    /**
     * @return server cpu usage formatted, or N/A if no monitor exists
     */
    private String getServerCpu() {
        if (rCpuMonitor == null) {
            return "N/A";
        } else {
            return String.format("%.1f", rCpuMonitor.getCpuUsage());
        }
    }

    /**
     * @return cpu usage formatted, or N/A if no monitor exists
     */
    private String getCpu() {
        if (cpuMonitor == null) {
            return "N/A";
        } else {
            return String.format("%.1f", cpuMonitor.getCpuUsage());
        }
    }

    /**
     * @return true if throughput histogram is collecting samples
     */
    public final boolean isSampling() {
        return isSampling;
    }

    /**
     * start sampling if not already sampling.
     */
    public final void startSampling() {
        isSampling = true;
    }

    /**
     * stop sampling.
     */
    public final void stopSampling() {
        isSampling = false;
    }

    /**
     * start monitoring the experiment.
     */
    public final synchronized void start() {
        if (monitorThread != null) {
            throw new IllegalStateException();
        }
        monitorThread = new Thread(this);
        monitorThread.setName("experiment-monitor-thread");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * stop the monitoring the experiment.
     */
    public final synchronized void stop() {
        if (monitorThread == null) {
            throw new IllegalStateException();
        }
        isRunning = false;
        try {
            LockSupport.unpark(monitorThread);
            monitorThread.join();
        } catch (InterruptedException e) {
            LOG.error("Interrupted while joining monitor thread", e);
        } finally {
            monitorThread = null;
        }
    }

    /**
     * @return the output stream used by this monitor
     */
    public final PrintStream getOutput() {
        return out;
    }
    
    /**
     * Depending on experiment config, warmup could be defined in different ways.
     * 
     * @return
     */
    private boolean warmupComplete() {

    	final boolean connectionsRampDone = experimentCounters.getCurrentlyConnected() >= getClientSettings().getMaxClients();
    	final boolean warmupMessagesDone = experimentCounters.getMessageCounter() > getClientSettings().getWarmupMessages(); //WARMUP_MESSAGES;
    	
		return warmupMessagesDone && connectionsRampDone ;
	}

	private CommonExperimentSettings getClientSettings() {
		return settings;
	}
}
