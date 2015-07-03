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

import java.util.HashSet;
import java.util.Set;

import com.pushtechnology.benchmarks.experiments.CommonExperimentSettings;
import com.pushtechnology.benchmarks.monitoring.ExperimentCounters;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.message.TopicMessage;


/**
 * A client which counts the number of available topics.
 *
 * @author nitsanw
 *
 */
public class TopicCountingClient extends MessageCountingClient {
    /**
     * topic set to count distinct topics observed by this client.
     */
    private final Set<String> topics = new HashSet<String>();
    /**
     * @param experimentCountersP shared experiment counters
     * @param initialTopicsP ...
     */
    public TopicCountingClient(ExperimentCounters experimentCountersP,
    		 CommonExperimentSettings clientSettings,
            String... initialTopicsP) {
        super(experimentCountersP, true, clientSettings, initialTopicsP);
    }

    @Override
    protected final void onMessage(ServerConnection serverConnection,
            TopicMessage topicMessage) {
        if (topics.add(topicMessage.getTopicName())) {
            experimentCounters.setTopicsCounter(topics.size());
        }
    }
}
