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

import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;


import com.pushtechnology.benchmarks.clients.ClientConnectionFactory;
import com.pushtechnology.benchmarks.clients.ExperimentClient;
import com.pushtechnology.benchmarks.monitoring.ExperimentCounters;
import com.pushtechnology.benchmarks.monitoring.ExperimentMonitor;
import com.pushtechnology.benchmarks.util.Factory;
import com.pushtechnology.diffusion.api.APIException;
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
public class ExperimentControlLoop implements Runnable {
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
    /** experiment counters used for reporting and load management. */
    private final ExperimentCounters experimentCounters =
            new ExperimentCounters();
    /** the monitor runs in the background and prints experiment output. */
    private final ExperimentMonitor experimentMonitor;
    /** common settings for control. */
    private final CommonExperimentSettings clientSettings;

    /** connection factory, will be initiated with a client factory. */
    private Factory<ExternalClientConnection> connector;
    /** load strategy to be drive load. */
    private ExperimentLoadStrategy loadStrategy;

    /**
     * @param settings ...
     */
    public ExperimentControlLoop(CommonExperimentSettings settings) {
        clientSettings = settings;

        experimentMonitor =
                new ExperimentMonitor(getExperimentCounters(),
                        settings.getOutputFile());
        setUp();

    }

    /**
     * @param clientFactory ...
     */
    public final void setClientFactory(
            Factory<ExperimentClient> clientFactory) {
        connector = new ClientConnectionFactory(
                getExperimentCounters(),
                getClientSettings(),
                clientFactory);
    }

    /**
     * @param strategy ...
     */
    public final void setLoadStartegy(ExperimentLoadStrategy strategy) {
        this.loadStrategy = strategy;
    }

    @Override
    public final void run() {
        try {
            // GO!
            long testStartTime = System.currentTimeMillis();
            Logs.info("Starting experiment");
            ExecutorService connectThread =
                    Executors.newSingleThreadExecutor();
            // generate initial load
            for (int i = 0; i < getClientSettings().getInitialClients(); i++) {
                connectThread.execute(createClientTask);
            }
            Logs.info("Initial load created");
            postInitialLoadCreated();
            experimentMonitor.startSampling();
            long lastIncrementTime = System.currentTimeMillis();
            while (loadStrategy.testNotOver(testStartTime)) {
                // periodically increase load if required
                if (loadStrategy.shouldIncrementLoad(lastIncrementTime)) {
                    lastIncrementTime = System.currentTimeMillis();
                    long currConns =
                            experimentCounters.getCurrentlyConnected();
                    int maxConns = getClientSettings().getMaxClients();
                    int incBy = getClientSettings().getClientIncrement();
                    incBy = (int) Math.min(incBy, maxConns - currConns);
                    if (incBy > 0) {
                        Logs.info("increasing load by:" + incBy);
                    }
                    for (int i = 0; i < incBy; i++) {
                        connectThread.execute(createClientTask);
                    }
                }
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            }
            connectThread.shutdownNow();
        } catch (Exception e) {
            Logs.severe("Error during experiment loop", e);
        }
        Logs.info("time is up, wrapping up");
        experimentMonitor.stop();
        connector.close();
        wrapupAndReport();
        Logs.info("experiment finished");
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
     */
    private void setUp() {
        if (Boolean.getBoolean("verbose")) {
            Logs.setLevel(Level.FINEST);
        } else {
            Logs.setLevel(Level.INFO);
        }
        // configure
        int qSize = getClientSettings().getMaxClients();
        int coreSize = getClientSettings().getInboundThreadPoolSize();
        try {
            setupThreadPool(qSize, coreSize);
        } catch (APIException e) {
            throw new RuntimeException(e);
        }
        experimentMonitor.start();
    }

    /**
     * setup the diffusion inbound thread pool.
     * 
     * @param qSize ...
     * @param coreSize ...
     * @throws APIException ...
     */
    private void setupThreadPool(int qSize, int coreSize)
            throws APIException {
        APIProperties.setInboundThreadPoolQueueSize(qSize);
        // this will allow us to set the max
        ThreadService.getInboundThreadPool().setCoreSize(0);
        // set the max
        ThreadService.getInboundThreadPool().setMaximumSize(coreSize);
        if (ThreadService.getInboundThreadPool().getMaximumSize() != coreSize) {
            throw new RuntimeException("Failed to set max pool size");
        }
        // core size MUST be set after max size :(
        ThreadService.getInboundThreadPool().setCoreSize(coreSize);
        if (ThreadService.getInboundThreadPool().getCoreSize() != coreSize) {
            throw new RuntimeException("Failed to set core pool size");
        }
    }

    /**
     * @return the client settings
     */
    protected final CommonExperimentSettings getClientSettings() {
        return clientSettings;
    }

    /**
     * @return the counters
     */
    public final ExperimentCounters getExperimentCounters() {
        return experimentCounters;
    }

    /**
     * Only to be used in the wrapup phase...
     * 
     * @return the experiment output stream
     */
    protected final PrintStream getOutput() {
        return experimentMonitor.getOutput();
    }

}
