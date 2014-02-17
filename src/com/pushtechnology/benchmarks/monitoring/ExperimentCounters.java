package com.pushtechnology.benchmarks.monitoring;

import java.util.concurrent.atomic.AtomicLong;

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
}

// CHECKSTYLE:ON
