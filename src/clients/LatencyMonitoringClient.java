/*
 * Copyright 2013 Push Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package clients;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.HdrHistogram.Histogram;

import monitoring.ExperimentCounters;

import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.message.MessageException;
import com.pushtechnology.diffusion.api.message.TopicMessage;

import experiments.EvilMessageAccessHack;

/**
 * This client will track message latency by reading the first 8 bytes of any
 * message received and adding to it's latency histogram. The timestamp is
 * in nano seconds.
 * 
 * @author nitsanw
 *
 */
public abstract class LatencyMonitoringClient extends MessageCountingClient {
    // CHECKSTYLE:OFF
    private static final int WARMUP_MESSAGES = 20000;
    private final Histogram histogram = 
            new Histogram(TimeUnit.SECONDS.toNanos(10), 3);
    protected ServerConnection connection;
    private Object connectionLock = new Object();
    
    public LatencyMonitoringClient(ExperimentCounters experimentCountersP,
            boolean reconnectP, String... initialTopicsP) {
        super(experimentCountersP, reconnectP, initialTopicsP);
    }

    @Override
    public final void onServerConnect(ServerConnection serverConnection) {
        synchronized (connectionLock) {
            this.connection = serverConnection;
        }
    }


    @Override
    public final void onMessage(ServerConnection serverConnection,
            TopicMessage topicMessage) {
        // CHECKSTYLE:ON
        long arrived = getArrivedTimestamp();

        if (experimentCounters.getMessageCounter() > WARMUP_MESSAGES
                && topicMessage.isDelta()) {
            try {
                long sent = getSentTimestamp(topicMessage);
                long rtt = arrived - sent;
                getHistogram().recordValue(rtt);
            } catch (Exception e) {
                Logs.severe("Failed to capture rtt:", e);
                return;
            }
        }
    }

    /**
     * @return ...
     */
    protected abstract long getArrivedTimestamp();

    /**
     * @param topicMessage ...
     * @return ...
     */
    protected abstract long getSentTimestamp(TopicMessage topicMessage);

    @Override
    public final void onServerDisconnect(ServerConnection serverConnection) {
        synchronized (connectionLock) {
            this.connection = null;
        }
    }

    public final void disconnect() {
        synchronized (connectionLock) {
            if (connection != null) {
                connection.close();
            }
        }
    }

    public final Histogram getHistogram() {
        return histogram;
    }
    // CHECKSTYLE:ON

}
