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
package com.pushtechnology.benchmarks.experiments;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.HdrHistogram.Histogram;

import com.pushtechnology.benchmarks.clients.ExperimentClient;
import com.pushtechnology.benchmarks.clients.PingClient;
import com.pushtechnology.benchmarks.publishers.PingClientSendPublisher;
import com.pushtechnology.benchmarks.util.Factory;

/**
 * A latency measuring experiment benchmarking the RTT latency of messages.
 * 
 * @author nitsanw
 * 
 */
public final class PingLatencyExperiment implements Runnable {
    /** the experiment loop. */
    private final ExperimentControlLoop loop;

    /**
     * client connections to be closed on close of factory and queried for
     * latency stats.
     */
    private final Set<PingClient> clients = Collections
            .newSetFromMap(new ConcurrentHashMap<PingClient, Boolean>());

    /**
     * @param settings ...
     * 
     */
    public PingLatencyExperiment(CommonExperimentSettings settings) {
        loop = new ExperimentControlLoop(settings) {
            @Override
            protected void wrapupAndReport() {
            	this.getExperimentCounters().reportLatency(getOutput());
            }
        };
        loop.setClientFactory(new Factory<ExperimentClient>() {
        	
        	int clientId = 0;
        	
            @Override
            public ExperimentClient create() {
            	
            	// If the client is pinging
            	String topicName = PingClientSendPublisher.ROOT_TOPIC;
            	if(loop.getClientSettings().isPingTopicSend()){
            		topicName += "_"+clientId++;
            	}
            	PingClient pingClient =
                        new PingClient(loop.getExperimentCounters(),
                                loop.getClientSettings().getMessageSize(),
                                loop.getClientSettings(),
                                topicName,null);
                clients.add(pingClient);
                return pingClient;
            }

            @Override
            public void close() {
                for (PingClient connection : clients) {
                    connection.disconnect();
                }
            }
        });
        ExperimentLoadStrategy defaultLoadStrategy =
                new DefaultLoadStrategy(loop.getClientSettings(),
                        loop.getExperimentCounters());
        loop.setLoadStartegy(defaultLoadStrategy);
    }

    @Override
    public void run() {
        loop.run();
    }
}
