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

import com.pushtechnology.benchmarks.clients.ExperimentClient;
import com.pushtechnology.benchmarks.clients.MessageCountingClient;
import com.pushtechnology.benchmarks.clients.TopicCountingClient;
import com.pushtechnology.benchmarks.monitoring.ExperimentCounters;
import com.pushtechnology.benchmarks.util.Factory;


/**
 * @author nitsanw
 * 
 */
public class DefaultClientFactory implements
        Factory<ExperimentClient> {
    /** configuration. */
    private final CommonExperimentSettings clientSettings;
    /** counters. */
    private final ExperimentCounters experimentCounters;

    /**
     * @param clientSettingsP ...
     * @param experimentCountersP ...
     */
    public DefaultClientFactory(CommonExperimentSettings clientSettingsP,
            ExperimentCounters experimentCountersP) {
        super();
        this.clientSettings = clientSettingsP;
        this.experimentCounters = experimentCountersP;
    }

    @Override
    public final ExperimentClient create() {
        if (experimentCounters.getConnectionAttemptsCounter() == 0) {
            return new TopicCountingClient(experimentCounters, clientSettings,
                    clientSettings.getRootTopic());
        } else {
            return new MessageCountingClient(experimentCounters,
                    false,
                    clientSettings,
                    clientSettings.getRootTopic());
        }
    }

    @Override
    public void close() {
    }
}
