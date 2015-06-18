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
package com.pushtechnology.benchmarks.clients;


import com.pushtechnology.benchmarks.experiments.CommonExperimentSettings;
import com.pushtechnology.benchmarks.monitoring.ExperimentCounters;
import com.pushtechnology.diffusion.api.Credentials;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.topic.TopicStatus;


/**
 * A client event listener setup with counters to support monitoring.
 * 
 * @author nitsanw
 * 
 */
public class MessageCountingClient implements ExperimentClient {
    // CHECKSTYLE:OFF docs will add nothing here
    protected final ExperimentCounters experimentCounters;
    private final String[] initialTopics;
    private volatile boolean reconnect;
    private final CommonExperimentSettings clientSettings;
    // CHECKSTYLE:ON

    /**
     * @param experimentCountersP shared counters for experiment
     * @param reconnectP should reconnect on connection failure
     * @param initialTopicsP initial topics to subscribe to on connection
     */
    public MessageCountingClient(ExperimentCounters experimentCountersP,
            boolean reconnectP, CommonExperimentSettings clientSettings, String... initialTopicsP) {
        this.experimentCounters = experimentCountersP;
        this.reconnect = reconnectP;
        initialTopics = initialTopicsP;
        this.clientSettings  = clientSettings;
    }

    @Override
    public final void serverConnected(final ServerConnection serverConnection) {
        onServerConnect(serverConnection);
        experimentCounters.incClientConnectCounter();
        afterServerConnect(serverConnection);
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
        experimentCounters.incMessageCounter();
        experimentCounters.incByteCounter(topicMessage.size());
        afterMessage(serverConnection, topicMessage);
    }

    /**
     * Allow extension of default behavior.
     * 
     * @param serverConnection ...
     * @param topicMessage ...
     */
    protected void afterMessage(ServerConnection serverConnection,
            TopicMessage topicMessage) {
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
    	int i=0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void serverDisconnected(ServerConnection serverConnection) {
        experimentCounters.incClientDisconnectCounter();
        onServerDisconnect(serverConnection);
        if (reconnect) {
            try {
                serverConnection.connect(initialTopics);
            } catch (Exception e) {
                if (Logs.isFinestLogging()) {
                    Logs.finest("Error on connection attempt", e);
                }
                experimentCounters.incConnectionRefusedCounter();
            }
        }
    }

    /**
     * method hook.
     * @param serverConnection ...
     */
    protected void onServerDisconnect(ServerConnection serverConnection) {
    	int i=0;
    }

    @Override
    public final String[] getInitialTopics() {
        return initialTopics;
    }

    public final CommonExperimentSettings getClientSettings() {
        return clientSettings;
    }

    /**
     * @param reconnectP set to false to stop client from reconnecting
     */
    public final void setReconnect(boolean reconnectP) {
        this.reconnect = reconnectP;
    }

    /**
     * post connection action.
     * @param serverConnection ...
     */
    public void afterServerConnect(ServerConnection serverConnection) {
    }
}
