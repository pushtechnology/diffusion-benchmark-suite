package monitoring;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A central reporting object shared between the clients/connection factory/
 * load strategy/monitoring thread for a given experiment.
 * 
 * @author nwakart
 * 
 */
public final class ExperimentCounters {
    // CHECKSTYLE:OFF
    private final AtomicLong messageCounter = new AtomicLong();
    private final AtomicLong bytesCounter = new AtomicLong();
    private final AtomicLong connectionAttemptsCounter = new AtomicLong();
    private final AtomicLong clientConnectCounter = new AtomicLong();
    private final AtomicLong clientDisconnectCounter = new AtomicLong();
    private final AtomicLong connectionRefusedCounter = new AtomicLong();
    private final AtomicLong topicsCounter = new AtomicLong();
    private final AtomicLong lastMessagesPerSecond = new AtomicLong(0L);

    // CHECKSTYLE:ON
    /**
     * @return number of currently connected (connected - disconnected)
     */
    public long getCurrentlyConnected() {
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
        return messageCounter.get();
    }

    public void incMessageCounter() {
        messageCounter.incrementAndGet();        
    }

    public long getBytesCounter() {
        return bytesCounter.get();
    }

    public void incByteCounter(int messageSize) {
        bytesCounter.addAndGet(messageSize);
    }
}
// CHECKSTYLE:ON
