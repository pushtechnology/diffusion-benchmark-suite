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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import publishers.PingPublisher;
import util.Histogram;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.Credentials;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.ServerConnectionListener;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.topic.TopicStatus;
import com.pushtechnology.diffusion.message.TopicMessageImpl;

public final class PingLatencyBenchmark extends DiffusionBenchmark {
    public static final long MAX_MESSAGES = 1000000;

    private final Set<ConnectionCallback> connections =
        Collections
            .newSetFromMap(new ConcurrentHashMap<ConnectionCallback,Boolean>());
    public static final int MESSAGE_SIZE;
    public static final byte[] MESSAGE;

    // init the benchmark setting from system properties
    static {
        MESSAGE_SIZE = Integer.getInteger("message.size", 125);
        MESSAGE = new byte[MESSAGE_SIZE];
    }

    public static void main(final String[] args) throws Exception {
        new PingLatencyBenchmark().benchmark();
        System.exit(0);
    }

    @Override
    protected void wrapup() throws InterruptedException {
        super.wrapup();
        Histogram histogramSummary = new Histogram(1024, 10000);
        for (ConnectionCallback connection : connections) {
            connection.disconnect();
            histogramSummary.addObservations(connection.histogram);
        }
        System.out.println(histogramSummary.toString());
    }

    @Override
    protected ServerConnectionListener getConnectionListener() {
        return new ConnectionCallback();
    }

    @Override
    protected void connectNewClient() {
        ConnectionCallback callback = new ConnectionCallback();
        connectAndPing(callback);
    }

    @Override
    protected void createFirstClient() {
        connectNewClient();
    }

    protected void connectAndPing(ConnectionCallback callback) {
        connector.createConnection(callback,
                PingPublisher.TOPIC);
//        if (conn != null) {
//            callback.ping(conn);
//        }
    }

    @Override
    protected boolean testNotOver(long testStartTime) {
        return super.testNotOver(testStartTime) &&
            messageCounter.get() < MAX_MESSAGES;
    }

    public final class ConnectionCallback implements
        ServerConnectionListener {
        Histogram histogram = new Histogram(1024, 10000);
        volatile long pingNanos;
        private ServerConnection serverConnection;

        public ConnectionCallback() {
            connections.add(this);
        }

        @Override
        public synchronized void serverConnected(
            final ServerConnection serverConnection) {
            PingLatencyBenchmark.this.clientConnectCounter.incrementAndGet();
            this.serverConnection = serverConnection;
            ping(serverConnection);
        }

        private void ping(final ServerConnection serverConnection) {
            try {
                TopicMessage message =
                    serverConnection
                        .createDeltaMessage(PingPublisher.TOPIC);
                message.put(MESSAGE);
                pingNanos = System.nanoTime();
                serverConnection.send(message);
            }
            catch (APIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void messageFromServer(final ServerConnection serverConnection,
            final TopicMessage topicMessage) {
            long rtt = System.nanoTime() - pingNanos;
            messageCounter.getAndIncrement();
            bytesCounter.addAndGet(((TopicMessageImpl) topicMessage)
                .getOriginalMessageSize());
            if (messageCounter.get() > 100000) {
                histogram.addObservation(rtt);
            }
            ping(serverConnection);
        }

        @Override
        public void serverTopicStatusChanged(
            final ServerConnection serverConnection,final String s,
            final TopicStatus topicStatus) {
        }

        @Override
        public void serverRejectedCredentials(
            final ServerConnection serverConnection,
            final Credentials credentials) {
        }

        @Override
        public synchronized void serverDisconnected(
            final ServerConnection serverConnection) {
            this.serverConnection = null;
            clientDisconnectCounter.incrementAndGet();
            connectAndPing(this);
        }

        public synchronized void disconnect() {
            if (serverConnection != null) {
                serverConnection.close();
            }
        }
    }
}
