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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import util.Factory;
import clients.ExperimentClient;
import clients.MessageCountingClient;

import com.pushtechnology.diffusion.api.ServerConnection;

public final class ConnectionChurnExperiment extends BaseExperiment {
    private final Set<ChurnClient> connections =
            Collections.newSetFromMap(
                    new ConcurrentHashMap<ChurnClient, Boolean>());
    private static final long CONNECTION_CHECK_TIME;
    private static final double DISCONNECT_PROBABILITY;
    // init the benchmark setting from system properties
    static {
        CONNECTION_CHECK_TIME =
                Long.getLong("connection.check.time",
                        TimeUnit.SECONDS.toMillis(10));
        DISCONNECT_PROBABILITY =
                Integer.getInteger("disconnect.probability", 5) / 100.0;
    }
    private final Timer disconnectTimer = new Timer();
    ConnectionChurnExperiment(){
        super();
        setClientFactory(new Factory<ExperimentClient>() {
            @Override
            public ExperimentClient create() {
                return new ChurnClient();
            }
        });
    }

    public final class ChurnClient extends MessageCountingClient {
        private ServerConnection connection;

        public ChurnClient() {
            super(ConnectionChurnExperiment.this.getExperimentCounters(), true);
            connections.add(this);
            TimerTask disconnectTask = new TimerTask() {
                @Override
                public void run() {
                    if (ThreadLocalRandom.current().nextDouble()
                    < DISCONNECT_PROBABILITY) {
                        disconnect();
                        cancel();
                    }

                }
            };
            disconnectTimer.schedule(disconnectTask, CONNECTION_CHECK_TIME,
                    CONNECTION_CHECK_TIME);
        }
        @Override
        protected void onServerConnect(ServerConnection serverConnection) {
            this.connection = serverConnection;
        }

        @Override
        public synchronized void onServerDisconnect(
                final ServerConnection serverConnection) {
            this.connection = null;
        }

        /**
         * Disconnects and sets a flag such that it stays disconnected.
         */
        public synchronized void disconnect() {
            if (connection != null) {
                setReconnect(false);
                connection.close();
            }
        }
    }

}
