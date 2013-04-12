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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.pushtechnology.diffusion.api.Credentials;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.ServerConnectionListener;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.topic.TopicStatus;
import com.pushtechnology.diffusion.message.TopicMessageImpl;

public final class ConnectionChurnBenchmark extends DiffusionBenchmark{
    private final Set<ConnectionCallback> connections =
         Collections.newSetFromMap(new ConcurrentHashMap<ConnectionCallback,Boolean>());
    private static final long CONNECTION_CHECK_TIME;
    private static final double DISCONNECT_PROBABILITY;
    // init the benchmark setting from system properties
    static {
        CONNECTION_CHECK_TIME = Long.getLong("connection.check.time", TimeUnit.SECONDS.toMillis(10));
        DISCONNECT_PROBABILITY = Integer.getInteger("disconnect.probability", 5)/100.0;
    }
    private final Timer disconnectTimer = new Timer();
    public static void main(final String[] args) throws Exception {
        new ConnectionChurnBenchmark().benchmark();
        System.exit(0);
    }

    @Override
    protected ServerConnectionListener getConnectionListener() {
        return new ConnectionCallback();
    }

    public final class ConnectionCallback implements
    ServerConnectionListener {
        private ServerConnection serverConnection;
        private boolean stayDisconnected = false;
        public ConnectionCallback() {
            connections.add(this);
            TimerTask disconnectTask = new TimerTask() {
                @Override
                public void run() {
                    if(ThreadLocalRandom.current().nextDouble() < DISCONNECT_PROBABILITY){
                        disconnect();
                        cancel();
                    }
                    
                }
            };
            disconnectTimer.schedule(disconnectTask, CONNECTION_CHECK_TIME, CONNECTION_CHECK_TIME);
        }

        @Override
        public synchronized void serverConnected(
        final ServerConnection serverConnection) {
            ConnectionChurnBenchmark.this.clientConnectCounter.incrementAndGet();
            this.serverConnection = serverConnection;
        }

        @Override
        public void messageFromServer(final ServerConnection serverConnection,
        final TopicMessage topicMessage) {
            messageCounter.getAndIncrement();
            bytesCounter.addAndGet(((TopicMessageImpl)topicMessage).getOriginalMessageSize());
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
            if(!stayDisconnected){
                connector.createConnection(this, DiffusionBenchmark.ROOT_TOPIC);
            }
        }

        public synchronized void disconnect() {
            if (serverConnection!=null) {
                stayDisconnected = true;
                serverConnection.close();
            }
        }
    }
}
