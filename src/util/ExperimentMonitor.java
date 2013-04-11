package util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class ExperimentMonitor implements Runnable
{
    private final AtomicLong messageCounter;
    private final AtomicLong bytesCounter;
    private final AtomicLong clientConnectCounter;
    private final AtomicLong clientDisconnectCounter;
    private final AtomicLong connectionAttemptsCounter;
    private final AtomicLong connectionRefusedCounter;
    private final AtomicLong topicsCounter;
    private final Histogram histogram;
    private final AtomicLong lastMessagesPerSecond = new AtomicLong(0L);
    private final CpuMonitor cpuMonitor = System.getProperty("java.vendor")
        .startsWith("Oracle") ? new CpuMonitor() : null;

    private volatile boolean running = true;
    private volatile boolean isSampling = false;

    public ExperimentMonitor(
        AtomicLong messageCounter,
        AtomicLong bytesCounter,
        AtomicLong clientCounter,
        AtomicLong clientDisconnectCounter,
        AtomicLong connectionAttemptsCounter,
        AtomicLong connectionRefusedCounter,
        AtomicLong topicsCounter)
    {
        this.messageCounter = messageCounter;
        this.bytesCounter = bytesCounter;
        this.clientConnectCounter = clientCounter;
        this.clientDisconnectCounter = clientDisconnectCounter;
        this.connectionAttemptsCounter = connectionAttemptsCounter;
        this.connectionRefusedCounter = connectionRefusedCounter;
        this.topicsCounter = topicsCounter;
        long[] ranges = new long[500];
        for (int i = 0;i < ranges.length;i++) {
            ranges[i] = 1 + i * 100000L;
        }
        histogram = new Histogram(ranges);
    }

    public void halt()
    {
        running = false;
    }

    public void startSampling() {
        isSampling = true;
    }

    public void stopSampling() {
        isSampling = false;
    }

    @Override
    public void run()
    {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
        System.out
            .println("Time, MessagesPerSecond, ClientsConnected, Topics, InSample, Cpu, ClientDisconnects,ConnectionRefusals,ConnectionAttempts,BytesPerSecond");
        long deadline = System.currentTimeMillis();
        while (running)
        {
            long messagesBefore = messageCounter.get();
            long bytesBefore = bytesCounter.get();
            long timeBefore = System.nanoTime();
            deadline += 1000;
            LockSupport.parkUntil(deadline);
            long intervalMessages = messageCounter.get() - messagesBefore;
            long intervalBytes = bytesCounter.get() - bytesBefore;
            long intervalNanos = System.nanoTime() - timeBefore;
            long messagesPerSecond =
                (long) intervalMessages * TimeUnit.SECONDS.toNanos(1) /
                    intervalNanos;
            long bytesPerSecond =
                (long) intervalBytes * TimeUnit.SECONDS.toNanos(1) /
                    intervalNanos;
            System.out.format("%s, %d, %d, %d, %b, %s, %d, %d, %d, %d\n",
                format.format(new Date()), messagesPerSecond,
                clientConnectCounter.get() - clientDisconnectCounter.get(),
                topicsCounter.get(), isSampling, getCpu(),
                clientDisconnectCounter.get(), connectionRefusedCounter.get(),
                connectionAttemptsCounter.get(), bytesPerSecond);
            if (isSampling) {
                histogram.addObservation(messagesPerSecond);
            }
            else {
                histogram.clear();
            }
            lastMessagesPerSecond.lazySet(messagesPerSecond);

        }
        System.out.println("-------");
        System.out.println(histogram.toThrouphputString(true));
    }

    private String getCpu() {
        if (cpuMonitor == null) {
            return "N/A";
        }
        else {
            return String.format("%.1f",cpuMonitor.getCpuUsage());
        }
    }

    public boolean isSampling() {
        return isSampling;
    }

    public long getMessagesPerSecond() {
        return lastMessagesPerSecond.get();
    }
}