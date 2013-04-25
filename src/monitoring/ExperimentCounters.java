package monitoring;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author nwakart
 *
 */
public class ExperimentCounters {
    public final AtomicLong messageCounter = new AtomicLong();
    public final AtomicLong bytesCounter = new AtomicLong();
    public final AtomicLong connectionAttemptsCounter = new AtomicLong();
    public final AtomicLong clientConnectCounter = new AtomicLong();
    public final AtomicLong clientDisconnectCounter = new AtomicLong();
    public final AtomicLong connectionRefusedCounter = new AtomicLong();
    public final AtomicLong topicsCounter = new AtomicLong();
    public final AtomicLong lastMessagesPerSecond = new AtomicLong(0L);

    public ExperimentCounters(){
        
    }
    /**
     * @return number of currently connected (connected - disconnected)
     */
    public final long getNumberCurrentlyConnected() {
        return clientConnectCounter.get() - clientDisconnectCounter.get();
    }
}
