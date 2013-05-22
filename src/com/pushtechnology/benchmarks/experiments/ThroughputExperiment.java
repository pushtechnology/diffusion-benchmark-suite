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
import com.pushtechnology.benchmarks.util.Factory;


/**
 * The basic throughput experiment.
 * 
 * @author nitsanw
 */
public final class ThroughputExperiment implements Runnable {

    /** the experiment loop. */
    private final ExperimentControlLoop loop;

    /**
     * @param settings ...
     */
    public ThroughputExperiment(CommonExperimentSettings settings) {
        loop = new ExperimentControlLoop(settings);
        Factory<ExperimentClient> clientFactory =
                new DefaultClientFactory(loop.getClientSettings(),
                        loop.getExperimentCounters());
        loop.setClientFactory(clientFactory);
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
