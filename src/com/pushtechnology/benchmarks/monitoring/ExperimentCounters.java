package com.pushtechnology.benchmarks.monitoring;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.ConcurrentHistogram;
import org.HdrHistogram.Histogram;

import com.pushtechnology.benchmarks.experiments.CommonExperimentSettings;
import com.pushtechnology.benchmarks.monitoring.LatencyMonitor.PeriodicLatencyHistogram;
import com.pushtechnology.benchmarks.util.LongAdder;

/**
 * A central reporting object shared between the clients/connection factory/
 * load strategy/monitoring thread for a given experiment.
 * 
 * @author nwakart
 * 
 */
public class ExperimentCounters {
    // CHECKSTYLE:OFF
    private final AtomicLong connectionAttemptsCounter = new AtomicLong(0L);
    private final AtomicLong clientConnectCounter = new AtomicLong(0L);
    private final AtomicLong clientDisconnectCounter = new AtomicLong(0L);
    private final AtomicLong connectionRefusedCounter = new AtomicLong(0L);
    private final AtomicLong topicsCounter = new AtomicLong(0L);
    private final AtomicLong lastMessagesPerSecond = new AtomicLong(0L);
    private final LongAdder messageCounter = new LongAdder();
    private final LongAdder bytesCounter = new LongAdder();

	private AtomicInteger clientQueueSizeTotal = new AtomicInteger(0);
	private AtomicInteger clientQueueSizeHighWatermarkTotal = new AtomicInteger(0);
	
	private AtomicInteger clientQueueSizeCounter = new AtomicInteger(0);
	private AtomicInteger clientQueueSizeHighWatermarkCounter = new AtomicInteger(0);
	
    private final Histogram messageThroughputHistogram =
            new Histogram(20 * 1000 * 1000, 3);
    
    LatencyMonitor latencyMonitor = new LatencyMonitor();
	private final CommonExperimentSettings settings;
    
    public ExperimentCounters(CommonExperimentSettings settings){
    	this.settings = settings;
    }
    
    // CHECKSTYLE:ON
    /**
     * NOTE this is an inaccurate measure reflecting recent values from the
     * connect and disconnect counters.
     * 
     * @return number of currently connected (connected - disconnected)
     */
    public final long getCurrentlyConnected() {
        return getClientConnectCounter() - getClientDisconnectCounter();
    }

    // CHECKSTYLE:OFF
    public long getLastMessagesPerSecond() {
        return lastMessagesPerSecond.get();
    }

    public void setLastMessagesPerSecond(long messagesPerSecond) {
        lastMessagesPerSecond.lazySet(messagesPerSecond);
    }

    public long getTopicsCounter() {
        return topicsCounter.get();
    }

    public void setTopicsCounter(int size) {
        topicsCounter.set(size);
    }

    public long getConnectionRefusedCounter() {
        return connectionRefusedCounter.get();
    }

    public void incConnectionRefusedCounter() {
        connectionRefusedCounter.incrementAndGet();
    }

    public long getClientDisconnectCounter() {
        return clientDisconnectCounter.get();
    }

    public void incClientDisconnectCounter() {
        clientDisconnectCounter.incrementAndGet();
    }

    public long getClientConnectCounter() {
        return clientConnectCounter.get();
    }

    public void incClientConnectCounter() {
        clientConnectCounter.incrementAndGet();
    }

    public long getConnectionAttemptsCounter() {
        return connectionAttemptsCounter.get();
    }

    public void incConnectionAttemptsCounter() {
        connectionAttemptsCounter.incrementAndGet();
    }
    public long getMessageCounter() {
        return messageCounter.sum();
    }

    public void incMessageCounter() {
        messageCounter.increment();
    }

    public long getBytesCounter() {
        return bytesCounter.sum();
    }

    public void incByteCounter(int messageSize) {
        bytesCounter.add(messageSize);
    }

	public AbstractHistogram getMessageThroughputHistogram() {
		return messageThroughputHistogram;
	}

	public void recordLatencyValue(long value) {
		
		if(getMessageCounter() > getClientSettings().getWarmupMessages()){
			warmupComplete();
		}
        latencyMonitor.recordLatencyValue(value);
	}
	
	private CommonExperimentSettings getClientSettings() {
		return settings;
	}

	public PeriodicLatencyHistogram getIntervalHistogram() {
		return latencyMonitor.getIntervalHistogram();
	}

	public void reportLatency(PrintStream printStream) {
		latencyMonitor.report(printStream);
	}

	public void warmupComplete() {
		latencyMonitor.warmupComplete();
	}
	
	public void sampleClientQueueSize(int qSize) {
		clientQueueSizeTotal.addAndGet(qSize);
		clientQueueSizeCounter.incrementAndGet();
	}
	
	public void sampleClientQueueSizeHighWatermark(int hwm) {
		clientQueueSizeHighWatermarkTotal.addAndGet(hwm);
		clientQueueSizeHighWatermarkCounter.incrementAndGet();
	}
	
	public int getAverageClientQueueSize() {
		if(clientQueueSizeCounter.get() == 0)
			return 0;
		else
			return clientQueueSizeTotal.getAndSet(0) / clientQueueSizeCounter.getAndSet(0);
	}
	
	public int getAverageClientQueueSizeHighWatermark() {
		if(clientQueueSizeHighWatermarkCounter.get() == 0)
			return 0;
		else
			return clientQueueSizeHighWatermarkTotal.getAndSet(0) / clientQueueSizeHighWatermarkCounter.getAndSet(0);
	}
    
}

// CHECKSTYLE:ON
