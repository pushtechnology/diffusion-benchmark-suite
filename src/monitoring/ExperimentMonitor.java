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
package monitoring;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class ExperimentMonitor implements Runnable {
    private final ExperimentCounters experimentCounters;
    private final Histogram histogram;
    private final CpuMonitor cpuMonitor = System.getProperty("java.vendor")
            .startsWith("Oracle") ? new CpuMonitor() : null;

    private volatile boolean running = true;
    private volatile boolean isSampling = false;
    private Thread monitorThread;

    public ExperimentMonitor(ExperimentCounters experimentCountersP) {
        this.experimentCounters = experimentCountersP;
        histogram = new Histogram(500, 100000);
    }

    @Override
    public final void run() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
        System.out.println("Time, MessagesPerSecond, ClientsConnected, Topics,"
                + " InSample, Cpu, ClientDisconnects, ConnectionRefusals, "
                + "ConnectionAttempts, BytesPerSecond");
        long deadline = System.currentTimeMillis();
        while (running) {
            long messagesBefore = experimentCounters.messageCounter.get();
            long bytesBefore = experimentCounters.bytesCounter.get();
            long timeBefore = System.nanoTime();
            deadline += 1000;
            LockSupport.parkUntil(deadline);
            long intervalMessages = experimentCounters.messageCounter.get()
                    - messagesBefore;
            long intervalBytes = experimentCounters.bytesCounter.get()
                    - bytesBefore;
            long intervalNanos = System.nanoTime() - timeBefore;
            long messagesPerSecond = (long) intervalMessages
                    * TimeUnit.SECONDS.toNanos(1) / intervalNanos;
            long bytesPerSecond = (long) intervalBytes
                    * TimeUnit.SECONDS.toNanos(1) / intervalNanos;
            System.out.format("%s, %d, %d, %d, %b, %s, %d, %d, %d, %d\n",
                    format.format(new Date()), messagesPerSecond,
                    experimentCounters.getNumberCurrentlyConnected(),
                    experimentCounters.topicsCounter.get(),
                    isSampling, getCpu(),
                    experimentCounters.clientDisconnectCounter.get(),
                    experimentCounters.connectionRefusedCounter.get(),
                    experimentCounters.connectionAttemptsCounter.get(),
                    bytesPerSecond);
            if (isSampling) {
                histogram.addObservation(messagesPerSecond);
            } else {
                histogram.clear();
            }
            // Single writer to this counter, so lazy set is fine
            experimentCounters.lastMessagesPerSecond.lazySet(messagesPerSecond);

        }
        System.out.println("-------");
        System.out.println(histogram.toThrouphputString(true));
    }

    /**
     * @return cpu usage formatted
     */
    private String getCpu() {
        if (cpuMonitor == null) {
            return "N/A";
        } else {
            return String.format("%.1f", cpuMonitor.getCpuUsage());
        }
    }

    public boolean isSampling() {
        return isSampling;
    }


    public void startSampling() {
        isSampling = true;
    }

    public void stopSampling() {
        isSampling = false;
    }

    public synchronized void start() {
        if (monitorThread != null) {
            throw new IllegalStateException();
        }
        monitorThread = new Thread(this);
        monitorThread.setName("experiment-monitor-thread");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public synchronized void stop() throws InterruptedException {
        if (monitorThread == null) {
            throw new IllegalStateException();
        }
        running = false;
        monitorThread.join();
        monitorThread = null;
    }
}
