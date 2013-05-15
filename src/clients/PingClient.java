package clients;

import java.nio.ByteBuffer;

import monitoring.ExperimentCounters;
import publishers.PingPublisher;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.message.TopicMessage;

/**
 * A ping latency experiment client. Sends a ping every time it receives a ping.
 * 
 * @author nitsanw
 */
public final class PingClient extends LatencyMonitoringClient {
    // CHECKSTYLE:OFF
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
    public void onMessage(ServerConnection serverConnection,
            TopicMessage topicMessage) {
        super.onMessage(serverConnection, topicMessage);
        ping();
    }

    @Override
    public void onServerConnect(ServerConnection serverConnection) {
        super.onServerConnect(serverConnection);
        ping();
    }

    /**
     * send a ping to server.
     */
    void ping() {
        try {
            TopicMessage m = connection
                    .createDeltaMessage(PingPublisher.ROOT_TOPIC,
                            message.length);
            messageBuffer.clear();
            messageBuffer.putLong(System.nanoTime());
            m.put(message);
            connection.send(m);
        } catch (APIException e) {
            if (Logs.isFinestLogging()) {
                Logs.finest("Error on trying to send a ping to server", e);
            }
        }
    }
}
