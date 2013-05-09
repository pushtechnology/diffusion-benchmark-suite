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
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import monitoring.Histogram;
import util.Factory;
import clients.ExperimentClient;
import clients.PingClient;


/**
 * A latency measuring experiment benchmarking the RTT latency of messages.
 * 
 * @author nitsanw
 *
 */
public final class PingLatencyExperiment implements Runnable {
    /** demonstrate specialized settings. */
    public static class Settings extends CommonExperimentSettings {
        /**
         * @param settings ...
         */
        public Settings(Properties settings) {
            super(settings);
            // add my own settings here
        }
    }
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
    public PingLatencyExperiment(Settings settings) {
        loop = new ExperimentControlLoop(settings) {
            @Override
            protected void wrapupAndReport() {
                // CHECKSTYLE:OFF
                Histogram histogramSummary = new Histogram(1024, 10000);
                // CHECKSTYLE:ON
                for (PingClient connection : clients) {
                    histogramSummary.addObservations(connection.getHistogram());
                }
                getOutput().println(histogramSummary.toString());
            }
        };
        loop.setClientFactory(new Factory<ExperimentClient>() {
            @Override
            public ExperimentClient create() {
                PingClient pingClient =
                        new PingClient(loop.getExperimentCounters(),
                                loop.getClientSettings().getMessageSize());
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
