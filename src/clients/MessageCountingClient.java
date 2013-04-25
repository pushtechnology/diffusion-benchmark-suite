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

import monitoring.ExperimentCounters;

import com.pushtechnology.diffusion.api.Credentials;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.topic.TopicStatus;
import com.pushtechnology.diffusion.message.TopicMessageImpl;


/**
 * A client event listener setup with counters to support monitoring. Can be
 * shared between many client connections.
 * 
 * @author nitsanw
 * 
 */
public class MessageCountingClient implements ExperimentClient {
    // CHECKSTYLE:OFF docs will add nothing here
    protected final ExperimentCounters experimentCounters;
    private boolean reconnect;
    private final String[] initialTopics;

    // CHECKSTYLE:ON

    /**
     * @param experimentCountersP shared counters for experiment
     * @param reconnectP should reconnect on connection failure
     * @param initialTopicsP initial topics to subscribe to on connection
     */
    public MessageCountingClient(ExperimentCounters experimentCountersP,
            boolean reconnectP, String... initialTopicsP) {
        this.experimentCounters = experimentCountersP;
        this.reconnect = reconnectP;
        initialTopics = initialTopicsP;
    }

    @Override
    public final void serverConnected(final ServerConnection serverConnection) {
        experimentCounters.clientConnectCounter.incrementAndGet();
        onServerConnect(serverConnection);
    }

    /**
     * Allow extension of default behavior.
     * 
     * @param serverConnection ...
     */
    protected void onServerConnect(ServerConnection serverConnection) {
    }

    @Override
    public final void messageFromServer(
            final ServerConnection serverConnection,
            final TopicMessage topicMessage) {
        onMessage(serverConnection, topicMessage);
        experimentCounters.messageCounter.incrementAndGet();
        // TODO: uses internal API
        experimentCounters.bytesCounter
                .addAndGet(((TopicMessageImpl) topicMessage)
                        .getOriginalMessageSize());
        

    }

    /**
     * Allow extension of default behavior.
     * 
     * @param serverConnection ...
     * @param topicMessage ...
     */
    protected void onMessage(ServerConnection serverConnection,
            TopicMessage topicMessage) {

    }

    @Override
    public void serverTopicStatusChanged(ServerConnection serverConnection,
            String s, TopicStatus topicStatus) {
    }

    @Override
    public void serverRejectedCredentials(ServerConnection serverConnection,
            Credentials credentials) {
    }

    @Override
    public final void serverDisconnected(ServerConnection serverConnection) {
        experimentCounters.clientDisconnectCounter.incrementAndGet();
        onServerDisconnect(serverConnection);
        if (reconnect) {
            try {
                serverConnection.connect(initialTopics);
            } catch (Exception e) {
                
                experimentCounters.connectionRefusedCounter.incrementAndGet();
            }
        }
    }

    /**
     * method hook.
     * @param serverConnection ...
     */
    protected void onServerDisconnect(ServerConnection serverConnection) {
    }

    @Override
    public final String[] getInitialTopics() {
        return initialTopics;
    }

    /**
     * @param reconnectP ...
     */
    public final void setReconnect(boolean reconnectP) {
        this.reconnect = reconnectP;
    }
}
