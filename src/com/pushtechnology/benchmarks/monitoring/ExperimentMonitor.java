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
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import org.HdrHistogram.Histogram;

import com.pushtechnology.benchmarks.util.JmxHelper;
import com.pushtechnology.benchmarks.util.Memory;
import com.pushtechnology.diffusion.api.Logs;

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
    private final Histogram messageThroughputHistogram =
            new Histogram(20 * 1000 * 1000, 3);
    private final CpuMonitor cpuMonitor = System.getProperty("java.vendor")
            .startsWith("Oracle") ? new LocalCpuMonitor() : null;
    private final MemoryMonitor memoryMonitor = new LocalMemoryMonitor();
    private final CpuMonitor rCpuMonitor;
    private final MemoryMonitor rMemoryMonitor;
    private final PrintStream out;

    private volatile boolean isRunning = true;
    private volatile boolean isSampling = false;
    private Thread monitorThread;
    private volatile long deadline;

    /**
     * Create monitor for experiment.
     * 
     * @param experimentCountersP Counters
     * @param outputFilename Name of output file
     * @param host Name of server host
     */
    @SuppressWarnings({ "resource", "deprecation" })
    public ExperimentMonitor(final ExperimentCounters experimentCountersP,
            final String outputFilename, final String host) {
        this.experimentCounters = experimentCountersP;
        RemoteMemoryMonitor m = null;
        RemoteCpuMonitor c = null;
        try {
            final JMXConnector connect =
                    JmxHelper.getJmxConnector(host, "jmxrmi", "guest",
                            "Guest");
            final MBeanServerConnection mBeanServerConnection =
                    connect.getMBeanServerConnection();
            m = new RemoteMemoryMonitor(mBeanServerConnection);
            c = new RemoteCpuMonitor(mBeanServerConnection);
        } catch (Exception e) {
            Logs.warning("Unable to create JMX connection to server", e);
        }
        rMemoryMonitor = m;
        rCpuMonitor = c;
        if (outputFilename == null || outputFilename.isEmpty()) {
            out = System.out;
        } else {
            PrintStream o;
            try {
                o = new PrintStream(outputFilename);
            } catch (FileNotFoundException e) {
                Logs.warning("failed to create output file: "
                        + outputFilename + " will use sysout instead.");
                o = System.out;
            }
            out = o;
        }
    }

    // CHECKSTYLE:ON
    @Override
    public final void run() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");

        getOutput().println("Time, MessagesPerSecond, ClientsConnected, Topics,"
                + " InSample, Cpu, ClientDisconnects, ConnectionRefusals, "
                + "ConnectionAttempts, BytesPerSecond, UsedHeapMB, "
                + "CommitedHeapMB, MaxHeapMB, UsedOffHeapMB, CommittedOffHeapMB"
                + ", MaxOffHeapMB, ServerCPU, ServerHeapUsedMB, "
                + "ServerHeapCommittedMB, ServerMaxHeapMB, ServerOffHeapUsedMB,"
                + " ServerOffHeapCommittedMB, ServerOffHeapMaxMB");
        deadline = System.currentTimeMillis();
        long messagesBefore = experimentCounters.getMessageCounter();
        long bytesBefore = experimentCounters.getBytesCounter();
        long timeBefore = System.nanoTime();
        while (isRunning) {
            deadline += MILLIS_IN_SECOND;
            LockSupport.parkUntil(deadline);
            final long messagesAfter = experimentCounters.getMessageCounter();
            final long bytesAfter = experimentCounters.getBytesCounter();
            final long timeAfter = System.nanoTime();
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

            memoryMonitor.sample();
            getOutput().format("%s, %d, %d, %d, %b, %s, %d, %d, %d, %d, %s, "
                    + "%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s\n",
                    format.format(new Date()),
                    messagesPerSecond,
                    experimentCounters.getCurrentlyConnected(),
                    experimentCounters.getTopicsCounter(),
                    isSampling,
                    getCpu(),
                    experimentCounters.getClientDisconnectCounter(),
                    experimentCounters.getConnectionRefusedCounter(),
                    experimentCounters.getConnectionAttemptsCounter(),
                    bytesPerSecond,
                    Memory.formatMemory(memoryMonitor.heapUsed()),
                    Memory.formatMemory(memoryMonitor.heapCommitted()),
                    Memory.formatMemory(memoryMonitor.heapMax()),
                    Memory.formatMemory(memoryMonitor.offHeapUsed()),
                    Memory.formatMemory(memoryMonitor.offHeapCommitted()),
                    Memory.formatMemory(memoryMonitor.offHeapMax()),
                    getServerCpu(),
                    getServerHeapUsed(),
                    getServerHeapCommitted(),
                    getServerHeapMax(),
                    getServerOffHeapUsed(),
                    getServerOffHeapCommitted(),
                    getServerOffHeapMax());
            if (isSampling) {
                messageThroughputHistogram.recordValue(messagesPerSecond);
            } else {
                messageThroughputHistogram.reset();
            }
            // Single writer to this counter, so lazy set is fine
            experimentCounters.setLastMessagesPerSecond(messagesPerSecond);
        }
        getOutput().println("-------");
        getOutput().print("Throughput [count: ");
        getOutput().print(messageThroughputHistogram.
                getHistogramData().getTotalCount());
        getOutput().print(" max:");
        getOutput().print(messageThroughputHistogram.
                getHistogramData().getMaxValue());
        getOutput().print(" avg:");
        getOutput().print(messageThroughputHistogram.
                getHistogramData().getMean());
        getOutput().print(" mid:");
        // CHECKSTYLE:OFF
        getOutput().print(messageThroughputHistogram.
                getHistogramData().getValueAtPercentile(50));
        // CHECKSTYLE:ON

        getOutput().print(" min:");
        getOutput().print(messageThroughputHistogram.
                getHistogramData().getMinValue());
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
        rMemoryMonitor.sample();
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
        rMemoryMonitor.sample();
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
    @SuppressWarnings("deprecation")
    public final synchronized void stop() {
        if (monitorThread == null) {
            throw new IllegalStateException();
        }
        isRunning = false;
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            Logs.severe("Interrupted while joining monitor thread", e);
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
}
