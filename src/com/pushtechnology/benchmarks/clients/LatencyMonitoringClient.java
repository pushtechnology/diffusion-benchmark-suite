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
import com.pushtechnology.benchmarks.publishers.PingClientSendPublisher;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.message.TopicMessage;

/**
 * This client will track message latency by reading the first 8 bytes of any
 * message received and adding to it's latency histogram. The timestamp is in
 * nano seconds.
 * 
 * @author nitsanw
 *
 */
public abstract class LatencyMonitoringClient extends MessageCountingClient {
	// CHECKSTYLE:OFF
	protected ServerConnection connection;
	private Object connectionLock = new Object();

	public LatencyMonitoringClient(ExperimentCounters experimentCountersP,
			boolean reconnectP, CommonExperimentSettings clientSettings,
			String... initialTopicsP) {
		super(experimentCountersP, reconnectP, clientSettings, initialTopicsP);
	}

	// CHECKSTYLE:ON
	@Override
	/**
	 * @param serverConnection The client connection
	 */
	public final void onServerConnect(ServerConnection serverConnection) {
		synchronized (connectionLock) {
			this.connection = serverConnection;
		}
	}

	/**
	 * Measures and records latency. See also ControlClientTLExperiment
	 * HISTOGRAM_SCALING_RATIO
	 * 
	 * @param serverConnection
	 *            The client connection
	 * @param topicMessage
	 *            The incoming message
	 */
	@SuppressWarnings("deprecation")
	@Override
	public final void onMessage(ServerConnection serverConnection,
			TopicMessage topicMessage) {


    	if (topicMessage.isDelta() && topicMessage.getTopicName().equals(PingClientSendPublisher.ROOT_TOPIC)) {
    		long arrived = getArrivedTimestamp();
		//if (topicMessage.isDelta()) {
			try {
				recordLatency(getSentTimestamp(topicMessage), arrived);
			} catch (Exception e) {
				Logs.severe("Failed to capture latency", e);
				return;
			}
		}
	}

	private void recordLatency(long sent, long arrived) {
		long rtt = arrived - sent;
		experimentCounters.recordLatencyValue(rtt);
	}

	/**
	 * @return ...
	 */
	protected abstract long getArrivedTimestamp();

	/**
	 * @param topicMessage
	 *            ...
	 * @return ...
	 */
	protected abstract long getSentTimestamp(TopicMessage topicMessage);

	@Override
	public final void onServerDisconnect(ServerConnection serverConnection) {
		synchronized (connectionLock) {
			this.connection = null;
		}
	}

	/**
	 * will disconnect if connected.
	 */
	public final void disconnect() {
		synchronized (connectionLock) {
			if (connection != null) {
				connection.close();
			}
		}
	}
}
