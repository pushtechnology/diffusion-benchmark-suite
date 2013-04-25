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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;

import monitoring.ExperimentCounters;
import monitoring.ExperimentMonitor;
import util.Factory;
import clients.ClientConnectionFactory;
import clients.ExperimentClient;

import com.pushtechnology.diffusion.api.APIProperties;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.client.ExternalClientConnection;
import com.pushtechnology.diffusion.api.threads.ThreadService;

/**
 * Base frame for experiments setting up a monitoring/reporting thread and
 * driving load up over time.
 * 
 * @author nwakart
 * 
 */
public class BaseExperiment implements Runnable {
    /** async connect client task. */
    private final Runnable createClientTask = new Runnable() {
        @Override
        public void run() {

            connector.create();
            long clientCreatePauseNanos = getClientSettings()
                    .getClientCreatePauseNanos();
            if (clientCreatePauseNanos > 0L) {
                LockSupport.parkNanos(clientCreatePauseNanos);
            }
        }
    };
    /** background thread for connection creation. */
    private static final Executor CONNECT_THREAD =
            Executors.newSingleThreadExecutor();
    /** experiment counters used for reporting and load management. */
    private final ExperimentCounters experimentCounters =
            new ExperimentCounters();
    /** the monitor runs in the background and prints experiment output. */
    private final ExperimentMonitor experimentMonitor =
            new ExperimentMonitor(getExperimentCounters());
    /** common settings for control. */
    private CommonClientSettings clientSettings;

    /** connection factory, will be initiated with a client factory. */
    private Factory<ExternalClientConnection> connector;
    /** load strategy to be drive load. */
    private ExperimentLoadStrategy loadStrategy;

    /**
     * TODO: externalize.
     */
    public BaseExperiment() {
        setClientSettings(new CommonClientSettings());
        Factory<ExperimentClient> clientFactory =
                new DefaultClientFactory(clientSettings, experimentCounters);
        setClientFactory(clientFactory);
        ExperimentLoadStrategy defaultLoadStrategy =
                new DefaultLoadStrategy(clientSettings);
        setLoadStartegy(defaultLoadStrategy);
    }

    /**
     * @param clientFactory ...
     */
    protected final void setClientFactory(
            Factory<ExperimentClient> clientFactory) {
        connector = new ClientConnectionFactory(
                getExperimentCounters(),
                getClientSettings(),
                clientFactory);
    }

    /**
     * @param strategy ...
     */
    protected final void setLoadStartegy(ExperimentLoadStrategy strategy) {
        this.loadStrategy = strategy;
    }

    /**
     * @param clientSettingsP ...
     */
    protected final void setClientSettings(CommonClientSettings 
            clientSettingsP) {
        clientSettings = clientSettingsP;
    }

    @Override
    public final void run() {
        try {
            setUp();
            // GO!
            long testStartTime = System.currentTimeMillis();

            // generate initial load
            for (int i = 0; i < getClientSettings().getInitialClients(); i++) {
                CONNECT_THREAD.execute(createClientTask);
            }
            postInitialLoadCreated();
            experimentMonitor.startSampling();
            long lastIncrementTime = System.currentTimeMillis();
            while (loadStrategy.testNotOver(testStartTime)) {
                // periodically increase load if required
                if (loadStrategy.shouldIncrementLoad(lastIncrementTime)) {
                    lastIncrementTime = System.currentTimeMillis();
                    long currConns =
                            experimentCounters.getNumberCurrentlyConnected();
                    int maxConns = getClientSettings().getMaxClients();
                    int incBy = getClientSettings().getClientIncrement();
                    incBy = (int) Math.min(incBy, maxConns - currConns);
                    for (int i = 0; i < incBy; i++) {
                        CONNECT_THREAD.execute(createClientTask);
                    }
                }
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            experimentMonitor.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        wrapupAndReport();
    }

    /**
     * A method hook for any logic to happen post initial load creation.
     */
    protected void postInitialLoadCreated() {
    }

    /**
     * A method hook for any end of experiment reporting.
     */
    protected void wrapupAndReport() {
    }

    /**
     * Setup some Diffusion properties and start monitoring.
     * 
     * @throws Exception should any badness happen
     */
    private void setUp() throws Exception {
        // TODO: remove hack
        System.setProperty("diffusion.use.external.data", "true");
        if (Boolean.getBoolean("verbose")) {
            Logs.setLevel(Level.FINEST);
        } else {
            Logs.setLevel(Level.OFF);
        }
        // configure
        int qSize = getClientSettings().getMaxClients();
        int coreSize = getClientSettings().getInboundThreadPoolCoreSize();
        int maxSize = getClientSettings().getInboundThreadPoolMaxSize();
        APIProperties.setInboundThreadPoolQueueSize(qSize);
        ThreadService.getInboundThreadPool().setCoreSize(coreSize);
        ThreadService.getInboundThreadPool().setMaximumSize(maxSize);
        experimentMonitor.start();
    }

    /**
     * @return the client settings
     */
    protected final CommonClientSettings getClientSettings() {
        return clientSettings;
    }

    /**
     * @return the counters
     */
    public final ExperimentCounters getExperimentCounters() {
        return experimentCounters;
    }
}
