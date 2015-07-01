package com.pushtechnology.benchmarks.clients;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.benchmarks.monitoring.ExperimentCounters;
import com.pushtechnology.diffusion.api.APIException;
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

    /** ping exchange topic. */
    private final String pingTopic;
    /** sent time. */
    private long sentTimeNanos;
    private static final Logger LOG = LoggerFactory.getLogger(PingClient.class);

    /**
     * @param experimentCountersP ...
     * @param sizeP message size
     * @param pingTopicP the topic on which we ping
     */
    public PingClient(ExperimentCounters experimentCountersP, int sizeP,
            String pingTopicP) {
        super(experimentCountersP, true,
                pingTopicP);
        this.size = sizeP;
        this.pingTopic = pingTopicP;
    }


    @Override
    public void afterMessage(ServerConnection serverConnection,
            TopicMessage topicMessage) {
        if (topicMessage.isDelta()) {
            ping(topicMessage);
        }
    }

    @Override
    public void afterServerConnect(ServerConnection serverConnection) {
        TopicMessage m;
        try {
            m = connection.createDeltaMessage(pingTopic, size);
            m.put(new byte[size]);
            ping(m);
        } catch (MessageException e) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Error on trying to send a ping to server", e);
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
            if (LOG.isTraceEnabled()) {
                LOG.trace("Error on trying to send a ping to server", e);
            }
        }
    }
    @Override
    protected long getSentTimestamp(TopicMessage topicMessage) {
        return sentTimeNanos;
    }


    @Override
    protected long getArrivedTimestamp() {
        return System.nanoTime();
    }
}
