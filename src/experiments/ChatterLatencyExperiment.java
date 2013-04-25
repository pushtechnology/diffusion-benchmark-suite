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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import monitoring.Histogram;
import publishers.ChatterPublisher;
import util.Factory;
import clients.ExperimentClient;
import clients.MessageCountingClient;
import clients.TopicCountingClient;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.ServerConnectionListener;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.message.DataMessageImpl;

/**
 * This is an experiment in loading the server with clients who chatter on a
 * given topic. When one sends a messages some reply (according to the reply
 * ratio) which triggers further chatter.
 * This exercises both input and out flows on the server and client side.
 * 
 * @author nitsanw
 *
 */
public final class ChatterLatencyExperiment extends BaseExperiment {
    public static final long MAX_MESSAGES = 100000000;

    private final Set<ConnectionCallback> connections =
            Collections
                    .newSetFromMap(new ConcurrentHashMap<ConnectionCallback, Boolean>());
    public static final double REPLY_RATIO;
    public static final int MESSAGE_SIZE;

    // init the benchmark setting from system properties
    static {
        MESSAGE_SIZE = Integer.getInteger("message.size", 125);
        REPLY_RATIO = Integer.getInteger("reply.percent", 5) / 100.0;
    }
    /**
     * 
     */
    public ChatterLatencyExperiment() {
        super();
        setClientFactory(new Factory<ExperimentClient>() {
            @Override
            public ExperimentClient create() {
                    return new ConnectionCallback();
            }
        });
    }

    @Override
    protected void wrapupAndReport() {
        Histogram histogramSummary = new Histogram(1024, 10000);
        for (ConnectionCallback connection : connections) {
            connection.disconnect();
            histogramSummary.addObservations(connection.histogram);
        }
        System.out.println(histogramSummary.toString());
    }
//
//
//    @Override
//    protected boolean testNotOver(long testStartTime) {
//        return super.testNotOver(testStartTime)
//                && experimentCounters.messageCounter.get() < MAX_MESSAGES;
//    }

    public final class ConnectionCallback extends MessageCountingClient {
        Histogram histogram = new Histogram(1024, 10000);
        volatile long pingNanos;
        private ServerConnection serverConnection;

        public ConnectionCallback() {
            super(ChatterLatencyExperiment.this.getExperimentCounters(),
                    true,
                    ChatterPublisher.TOPIC);
            connections.add(this);
        }

        @Override
        protected synchronized void onServerConnect(
                ServerConnection serverConnectionP) {
            this.serverConnection = serverConnectionP;
            ping();
        }

        /**
         * @param serverConnectionP
         */
        private void ping() {
            try {
                TopicMessage message = serverConnection
                        .createDeltaMessage(ChatterPublisher.TOPIC);
                ByteBuffer wrap = ByteBuffer.wrap(new byte[MESSAGE_SIZE]);
                wrap.putLong(System.nanoTime());
                wrap.clear();
                message.put(wrap);
                serverConnection.send(message);
            } catch (APIException e) {
                serverConnection.close();
            }
        }

        @Override
        public void onMessage(final ServerConnection serverConnectionP,
                final TopicMessage topicMessage) {
            long sent = ByteBuffer.wrap(
                    ((DataMessageImpl) topicMessage).getExternalData())
                    .getLong();
            long rtt = (System.nanoTime() - sent);
            if (experimentCounters.messageCounter.get() > 100000) {
                histogram.addObservation(rtt);
            }
            if (ThreadLocalRandom.current().nextDouble() < REPLY_RATIO) {
                ping();
            }
        }

        @Override
        public synchronized void onServerDisconnect(
                ServerConnection serverConnectionP) {
            this.serverConnection = null;
        }

        /**
         * 
         */
        public synchronized void disconnect() {
            if (serverConnection != null) {
                serverConnection.close();
            }
        }
    }
}
