package clients;

import java.nio.ByteBuffer;

import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.message.MessageException;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.message.DataMessageImpl;

import monitoring.ExperimentCounters;
import monitoring.Histogram;

public class LatencyMonitoringClient extends MessageCountingClient {
    private static final int WARMUP_MESSAGES = 20000;
    private final Histogram histogram = new Histogram(1024, 10000);
    protected ServerConnection connection;
    private Object connectionLock = new Object();
    private byte[] timestamp = new byte[8];
    private ByteBuffer tBuffy = ByteBuffer.wrap(timestamp);
    
    public LatencyMonitoringClient(ExperimentCounters experimentCountersP,
            boolean reconnectP, String... initialTopicsP) {
        super(experimentCountersP, reconnectP, initialTopicsP);
    }

    @Override
    public void onServerConnect(ServerConnection serverConnection) {
        synchronized (connectionLock) {
            this.connection = serverConnection;
        }
    }

    // CHECKSTYLE:OFF
    @Override
    public void onMessage(ServerConnection serverConnection,
            TopicMessage topicMessage) {
        // CHECKSTYLE:ON
        long arrived = System.nanoTime();

        if (experimentCounters.getMessageCounter() > WARMUP_MESSAGES
                && topicMessage.isDelta()) {
            try {
                topicMessage.nextBytes(timestamp);
            } catch (MessageException e) {
                return;
            }    
            long sent = tBuffy.getLong(0);

            long rtt = arrived - sent;
            getHistogram().addObservation(rtt);
        }
    }

    // CHECKSTYLE:OFF
    @Override
    public void onServerDisconnect(ServerConnection serverConnection) {
        synchronized (connectionLock) {
            this.connection = null;
        }
    }

    public void disconnect() {
        synchronized (connectionLock) {
            if (connection != null) {
                connection.close();
            }
        }
    }

    public Histogram getHistogram() {
        return histogram;
    }
    // CHECKSTYLE:ON

}
