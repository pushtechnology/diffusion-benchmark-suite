package clients;

import java.nio.ByteBuffer;

import monitoring.ExperimentCounters;
import monitoring.Histogram;
import publishers.PingPublisher;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.message.DataMessageImpl;

/**
 * A ping latency experiment client. Sends a ping every time it receives a ping.
 * 
 * @author nitsanw
 */
public final class PingClient extends MessageCountingClient {
    // CHECKSTYLE:OFF
    private static final int WARMUP_MESSAGES = 20000;
    private final Histogram histogram = new Histogram(1024, 10000);
    private ServerConnection connection;
    private final byte[] message;
    private final ByteBuffer messageBuffer;

    public PingClient(ExperimentCounters experimentCountersP, int size) {
        // CHECKSTYLE:ON
        super(experimentCountersP, true,
                PingPublisher.ROOT_TOPIC);
        message = new byte[size];
        messageBuffer = ByteBuffer.wrap(message);
    }

    @Override
    public synchronized void onServerConnect(
            final ServerConnection serverConnection) {
        this.connection = serverConnection;
        ping();
    }

    /**
     * send a ping to server.
     */
    private void ping() {
        try {
            TopicMessage m = connection
                    .createDeltaMessage(PingPublisher.ROOT_TOPIC, 
                            message.length);
            messageBuffer.clear();
            messageBuffer.putLong(System.nanoTime());
            m.put(message);
            connection.send(m);
        } catch (APIException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(final ServerConnection serverConnection,
            final TopicMessage topicMessage) {
        // TODO: hack, should be made available via a read only BB.
        byte[] externalData = 
                ((DataMessageImpl) topicMessage).getExternalData();
        if (externalData.length != message.length) {
            return;
        }
        long sent = ByteBuffer.wrap(externalData).getLong();
        long rtt = System.nanoTime() - sent;
        if (experimentCounters.getMessageCounter() > WARMUP_MESSAGES) {
            getHistogram().addObservation(rtt);
        }
        ping();
    }

    @Override
    public synchronized void onServerDisconnect(
            ServerConnection serverConnection) {
        this.connection = null;
    }

    // CHECKSTYLE:OFF
    public synchronized void disconnect() {
        if (connection != null) {
            connection.close();
        }
    }

    public Histogram getHistogram() {
        return histogram;
    }
    // CHECKSTYLE:ON
}
