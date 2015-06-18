package com.pushtechnology.benchmarks.clients;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.pushtechnology.benchmarks.experiments.CommonExperimentSettings;
import com.pushtechnology.benchmarks.monitoring.ExperimentCounters;
import com.pushtechnology.diffusion.api.Credentials;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.topic.TopicStatus;

public class PingAndThroughputClient implements ExperimentClient {

	PingClient pingClient;
	MessageCountingClient throughputClient;
	
	ExperimentCounters experimentCounters;
	
	public PingAndThroughputClient(ExperimentCounters experimentCounters, int size,
    		CommonExperimentSettings settings,
            String pingTopic, String publishTopic,
            ScheduledThreadPoolExecutor sched) {
		
		this.experimentCounters = experimentCounters;
		
		pingClient =
                new PingClient(experimentCounters,
                		size,
                		settings,
                		pingTopic,
                		sched);
		
		throughputClient = new MessageCountingClient(experimentCounters,
                false,
                settings,
                //settings.getRootTopic()
                publishTopic);
    }

    @Override
    public final void serverConnected(final ServerConnection serverConnection) {
    	throughputClient.onServerConnect(serverConnection);
    	pingClient.onServerConnect(serverConnection);
        experimentCounters.incClientConnectCounter();
        pingClient.afterServerConnect(serverConnection);
    }

    @Override
    public final void messageFromServer(
            final ServerConnection serverConnection,
            final TopicMessage topicMessage) {
    	throughputClient.onMessage(serverConnection, topicMessage);
    	pingClient.onMessage(serverConnection, topicMessage);
        experimentCounters.incMessageCounter();
        experimentCounters.incByteCounter(topicMessage.size());
        pingClient.afterMessage(serverConnection, topicMessage);
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void serverDisconnected(ServerConnection serverConnection) {
        experimentCounters.incClientDisconnectCounter();
        throughputClient.onServerDisconnect(serverConnection);
    }

    @Override
    public final String[] getInitialTopics() {
    	
    	int topics = throughputClient.getInitialTopics().length + pingClient.getInitialTopics().length;
    	String[] initialTopics = new String[topics];
    	int topicIndex=0;
    	for(int i=0;i<throughputClient.getInitialTopics().length;i++,topicIndex++){
    		initialTopics[topicIndex] = throughputClient.getInitialTopics()[i];
    	}
    	for(int i=0;i<pingClient.getInitialTopics().length;i++,topicIndex++){
    		initialTopics[topicIndex] = pingClient.getInitialTopics()[i];
    	}
    	
        return initialTopics;
    }

    public final CommonExperimentSettings getClientSettings() {
        return throughputClient.getClientSettings();
    }

    /**
     * @param reconnectP set to false to stop client from reconnecting
     */
    public final void setReconnect(boolean reconnectP) {
    	throughputClient.setReconnect(reconnectP);
    }

    /**
     * post connection action.
     * @param serverConnection ...
     */
    public void afterServerConnect(ServerConnection serverConnection) {
    	pingClient.afterServerConnect(serverConnection);
    }

	/**
	 * will disconnect if connected.
	 */
	public final void disconnect() {
		this.pingClient.disconnect();
	}

    /**
     * method hook.
     * @param serverConnection ...
     */
    protected void onServerDisconnect(ServerConnection serverConnection) {
    	int i=0;
    }
    
	@Override
	public void serverRejectedCredentials(ServerConnection arg0,
			Credentials arg1) {
		int i=0;
	}

	@Override
	public void serverTopicStatusChanged(ServerConnection arg0, String arg1,
			TopicStatus arg2) {
	}
    
}
