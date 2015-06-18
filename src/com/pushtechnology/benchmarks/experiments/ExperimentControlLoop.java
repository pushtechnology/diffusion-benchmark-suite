/*
 * Copyright 2013, 2014 Push Technology
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.pushtechnology.benchmarks.clients.ClientConnectionFactory;
import com.pushtechnology.benchmarks.clients.ExperimentClient;
import com.pushtechnology.benchmarks.monitoring.ExperimentCounters;
import com.pushtechnology.benchmarks.monitoring.ExperimentMonitor;
import com.pushtechnology.benchmarks.util.Factory;
import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.client.ExternalClientConnection;
import com.pushtechnology.diffusion.api.config.ConfigManager;
import com.pushtechnology.diffusion.api.config.ThreadPoolConfig;
import com.pushtechnology.diffusion.api.config.ThreadsConfig;
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
            if (!shouldAttemptMoreConnections()) {
                return;
            }
            connector.create();
            final long clientCreatePauseNanos = getClientSettings()
                    .getClientCreatePauseNanos();
            if (clientCreatePauseNanos > 0L) {
                LockSupport.parkNanos(clientCreatePauseNanos);
            }
        }
    };
    /** experiment counters used for reporting and load management. */
    private final ExperimentCounters experimentCounters;
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
    public ExperimentControlLoop(final CommonExperimentSettings settings) {
        clientSettings = settings;

        experimentCounters =
                new ExperimentCounters(settings);
        
        experimentMonitor =
                new ExperimentMonitor(getExperimentCounters(), settings);
        setUp();

    }

    /**
     * @param clientFactory ...
     */
    public final void setClientFactory(
            final Factory<ExperimentClient> clientFactory) {
        connector = new ClientConnectionFactory(
                getExperimentCounters(),
                getClientSettings(),
                clientFactory);
    }

    /**
     * @param strategy ...
     */
    public final void setLoadStartegy(final ExperimentLoadStrategy strategy) {
        this.loadStrategy = strategy;
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void run() {
        try {
            // GO!
            final long testStartTime = System.currentTimeMillis();
            Logs.info("Starting experiment");
            final int connectQCapacity =
                    getClientSettings().getInitialClients()
                            + 2 * getClientSettings().getClientIncrement();
            final BlockingQueue<Runnable> connectQ =
                    new ArrayBlockingQueue<Runnable>(connectQCapacity);
            final ThreadPoolExecutor connectThread =
                    new ThreadPoolExecutor(10, 100, 10, TimeUnit.SECONDS,
                            connectQ);
            // generate initial load
            connectThread
                    .setRejectedExecutionHandler(
                            new RejectedExecutionHandler() {
                                @Override
                                public void rejectedExecution(Runnable r,
                                        ThreadPoolExecutor executor) {
                                    // DO NOTHING
                                }
                            });
            for (int i = 0; i < getClientSettings().getInitialClients(); i++) {
                connectThread.execute(createClientTask);
            }
            Logs.info("Initial load created");
            postInitialLoadCreated();
            experimentMonitor.start();
            experimentMonitor.startSampling();
            long lastIncrementTime = System.currentTimeMillis();
            while (loadStrategy.testNotOver(testStartTime)) {
                // periodically increase load if required
                if (loadStrategy.shouldIncrementLoad(lastIncrementTime)) {
                    lastIncrementTime = System.currentTimeMillis();
                    final long currConns =
                            experimentCounters.getCurrentlyConnected();
                    final int maxConns = getClientSettings().getMaxClients();
                    int incBy = getClientSettings().getClientIncrement();
                    incBy = (int) Math.min(incBy, maxConns - currConns);
                    if (incBy > 0) {
                        Logs.info("increasing load by:" + incBy);
                    } else {
                    	experimentCounters.warmupComplete();
                    }
                    for (int i = 0; i < incBy; i++) {
                        connectThread.execute(createClientTask);
                    }
                }
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            }
            connectThread.shutdownNow();
        } catch (final Exception e) {
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
     * Setup some Diffusion properties
     */
    private void setUp() {
        // configure
        final int qSize = getClientSettings().getMaxClients();
        final int coreSize = getClientSettings().getInboundThreadPoolSize();
        try {
            setupThreadPool(qSize, coreSize);
        } catch (final APIException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * setup the diffusion inbound thread pool.
     * 
     * @param qSize ...
     * @param coreSize ...
     * @throws APIException ...
     */
    private void setupThreadPool(final int qSize, final int coreSize)
            throws APIException {
    	ThreadsConfig threadsConfig = ConfigManager.getConfig().getThreads();
    	ThreadPoolConfig pool = threadsConfig.addPool("in");
    	pool.setQueueSize(qSize);
    	threadsConfig.setInboundPool("in");
		
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

    /**
     * Decides if more connections should be made to the server.
     *
     * @return {@code true} if more connections should be attempted.
     */
    private boolean shouldAttemptMoreConnections() {
        final long currOutstandingConns =
                experimentCounters.getConnectionAttemptsCounter()
                        - (experimentCounters.getClientDisconnectCounter()
                        + experimentCounters.getConnectionRefusedCounter());
        final int maxConns = getClientSettings().getMaxClients();
        return maxConns > currOutstandingConns;
    }

}
