package clients;

import monitoring.ExperimentCounters;
import publishers.PingPublisher;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.message.MessageException;
import com.pushtechnology.diffusion.api.message.TopicMessage;

/**
 * A ping latency experiment client. Sends a ping every time it receives a ping.
 * 
 * @author nitsanw
 */
public final class PingClient extends LatencyMonitoringClient {
    /** message size. */
    private final int size;
    /** sent time. */
    private long sentTimeNanos;

    /**
     * @param experimentCountersP ...
     * @param sizeP message size
     */
    public PingClient(ExperimentCounters experimentCountersP, int sizeP) {
        super(experimentCountersP, true,
                PingPublisher.ROOT_TOPIC);
        this.size = sizeP;
    }

    @Override
    public void onMessage(ServerConnection serverConnection,
            TopicMessage topicMessage) {
        super.onMessage(serverConnection, topicMessage);
        ping(topicMessage);
    }

    @Override
    public void onServerConnect(ServerConnection serverConnection) {
        super.onServerConnect(serverConnection);
        TopicMessage m;
        try {
            m = connection
                    .createDeltaMessage(PingPublisher.ROOT_TOPIC, size);
            m.put(new byte[size]);
            ping(m);
        } catch (MessageException e) {
            if (Logs.isFinestLogging()) {
                Logs.finest("Error on trying to send a ping to server", e);
            }
        }
    }

    /**
     * send a ping to server.
     * @param topicMessage 
     */
    void ping(TopicMessage topicMessage) {
        try {
            sentTimeNanos = System.nanoTime();
            connection.send(topicMessage);
        } catch (APIException e) {
            if (Logs.isFinestLogging()) {
                Logs.finest("Error on trying to send a ping to server", e);
            }
        }
    }
    @Override
    protected long getSentTimestamp(TopicMessage topicMessage)
            throws MessageException {
        return sentTimeNanos;
    }
}
