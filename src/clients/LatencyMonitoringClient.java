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

/**
 * This client will track message latency by reading the first 8 bytes of any
 * message received and adding to it's latency histogram. The timestamp is
 * in nano seconds.
 * 
 * @author nitsanw
 *
 */
public class LatencyMonitoringClient extends MessageCountingClient {
    // CHECKSTYLE:OFF
    private static final int WARMUP_MESSAGES = 20000;
    private final Histogram histogram = 
            new Histogram(TimeUnit.SECONDS.toNanos(10), 3);
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
            if(rtt > TimeUnit.MINUTES.toNanos(1)){
                Logs.severe("WTF:"+sent);
                return;
            }
            getHistogram().recordValue(rtt);
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
