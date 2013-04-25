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
package experiments;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import monitoring.Histogram;

import publishers.PingPublisher;
import util.Factory;

import clients.ExperimentClient;
import clients.MessageCountingClient;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.Credentials;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.ServerConnectionListener;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.topic.TopicStatus;
import com.pushtechnology.diffusion.message.TopicMessageImpl;

import experiments.ChatterLatencyExperiment.ConnectionCallback;

public final class PingLatencyExperiment extends BaseExperiment {
    public static final long MAX_MESSAGES = 1000000;

    private final Set<PingClient> clients = Collections
            .newSetFromMap(new ConcurrentHashMap<PingClient, Boolean>());
    public static final int MESSAGE_SIZE;
    public static final byte[] MESSAGE;

    // init the benchmark setting from system properties
    static {
        MESSAGE_SIZE = Integer.getInteger("message.size", 125);
        MESSAGE = new byte[MESSAGE_SIZE];
    }

    /**
     * 
     */
    public PingLatencyExperiment() {
        super();
        setClientFactory(new Factory<ExperimentClient>() {
            @Override
            public ExperimentClient create() {
                return new PingClient();
            }
        });
        setLoadStartegy(new DefaultLoadStrategy(getClientSettings()) {
            @Override
            public boolean testNotOver(long testStartTime) {
                return super.testNotOver(testStartTime)
                        && getExperimentCounters().messageCounter.get() < MAX_MESSAGES;
            }
        });
    }

    @Override
    protected void wrapupAndReport() {
        Histogram histogramSummary = new Histogram(1024, 10000);
        for (PingClient connection : clients) {
            connection.disconnect();
            histogramSummary.addObservations(connection.histogram);
        }
        System.out.println(histogramSummary.toString());
    }

    public final class PingClient extends MessageCountingClient {
        Histogram histogram = new Histogram(1024, 10000);
        long pingNanos;
        private ServerConnection connection;

        public PingClient() {
            super(PingLatencyExperiment.this.getExperimentCounters(), true,
                    PingPublisher.TOPIC);
            clients.add(this);
        }

        @Override
        public synchronized void onServerConnect(
                final ServerConnection serverConnection) {
            this.connection = serverConnection;

            ping();
        }

        private void ping() {
            try {
                TopicMessage message = connection
                        .createDeltaMessage(PingPublisher.TOPIC);
                message.put(MESSAGE);
                pingNanos = System.nanoTime();
                connection.send(message);
            } catch (APIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void onMessage(final ServerConnection serverConnection,
                final TopicMessage topicMessage) {
            long rtt = System.nanoTime() - pingNanos;
            if (experimentCounters.messageCounter.get() > 10000) {
                histogram.addObservation(rtt);
            }
            ping();
        }

        @Override
        public synchronized void onServerDisconnect(
                ServerConnection serverConnection) {
            this.connection = null;
        }

        public synchronized void disconnect() {
            if (connection != null) {
                connection.close();
            }
        }
    }
}
