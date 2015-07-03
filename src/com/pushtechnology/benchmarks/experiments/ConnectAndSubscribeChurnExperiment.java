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

import static com.pushtechnology.benchmarks.util.PropertiesUtil.getProperty;

import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.benchmarks.clients.ExperimentClient;
import com.pushtechnology.benchmarks.clients.MessageCountingClient;
import com.pushtechnology.benchmarks.util.Factory;
import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.ServerConnection;

/**
 * A latency measuring experiment benchmarking the RTT latency of messages.
 *
 * @author nitsanw
 *
 */
public final class ConnectAndSubscribeChurnExperiment implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectAndSubscribeChurnExperiment.class);
    /**
     * Probability that a subscription event will be a subscribe not an
     * unsubscribe.
     */
    private static final double SUBSCRIBE_PROBABILITY = 0.5;
    /** the experiment loop. */
    private final ExperimentControlLoop loop;

    /** Experiment specialized settings. */
    public static class Settings extends CommonExperimentSettings {
        // CHECKSTYLE:OFF
        private final double disconnectProbability;
        private final double disconnectCheckInterval;
        private final String subscriptionsRoot;
        private final int topicCount;
        private final double subscribeEventProbability;
        private final double subscribeCheckInterval;

        public Settings(Properties settings) {
            super(settings);
            disconnectProbability = getProperty(settings, "disconnect.probability", 0.2);
            disconnectCheckInterval = getProperty(settings, "disconnect.check.interval", 1.0);
            subscriptionsRoot = getProperty(settings, "subscriptions.root", "MANY");
            topicCount = getProperty(settings, "topic.count", 100);
            subscribeEventProbability = getProperty(settings, "subscribeEventProbability", 0.2);
            subscribeCheckInterval = getProperty(settings, "subscribeCheckInterval", 0.1);
        }
        // CHECKSTYLE:ON
    }

    /**
     * Thread local random number generator. Uses the thread ID as the seed so
     * generators provide difference sequences for each thread. 
     */
    private final ThreadLocal<Random> tlr = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random(Thread.currentThread().getId());
        }
    };

    /**
     * @param settings ...
     *
     */
    public ConnectAndSubscribeChurnExperiment(final Settings settings) {
        final Timer timer = new Timer();
        final int disconnectCheckIntervalMs =
                (int) (settings.disconnectCheckInterval * 1000);
        final int subscribeCheckIntervalMs =
                (int) (settings.subscribeCheckInterval * 1000);
        final String[] topics = new String[settings.topicCount];
        for (int i = 0; i < settings.topicCount; i++) {
            topics[i] = settings.subscriptionsRoot + "/" + i;
        }
        loop = new ExperimentControlLoop(settings) {
            @Override
            protected void wrapupAndReport() {
                timer.cancel();
            }
        };
        loop.setClientFactory(new Factory<ExperimentClient>() {
            @Override
            public ExperimentClient create() {
                MessageCountingClient client = new MessageCountingClient(
                        loop.getExperimentCounters(),
                        false,
                        loop.getClientSettings(),
                        settings.getRootTopic()) {
                    @Override
                    public void afterServerConnect(
                            final ServerConnection serverConnection) {
                        final TimerTask subscribeTask = new TimerTask() {
                            @Override
                            public void run() {
                                if (nextDouble() < settings
                                        .subscribeEventProbability) {
                                    try {
                                        if (nextDouble()
                                                < SUBSCRIBE_PROBABILITY) {
                                            serverConnection.subscribe(
                                                topics[nextInt(
                                                        settings.topicCount)]);
                                        } else {
                                            serverConnection.unsubscribe(
                                                topics[nextInt(
                                                        settings.topicCount)]);
                                        }
                                    } catch (final APIException e) {
                                        LOG.trace(
                                            "Exception performing"
                                            + "subscription event",
                                            e);
                                    }
                                }
                            }
                        };
                        TimerTask disconnectTask = new TimerTask() {
                            @Override
                            public void run() {
                                if (nextDouble()
                                        < settings.disconnectProbability
                                        || !serverConnection.isConnected()) {
                                    serverConnection.close();
                                    this.cancel();
                                    subscribeTask.cancel();
                                }
                            }
                        };
                        timer.schedule(
                            disconnectTask,
                            nextInt(disconnectCheckIntervalMs),
                            disconnectCheckIntervalMs);
                        timer.schedule(
                            subscribeTask,
                            nextInt(subscribeCheckIntervalMs),
                            subscribeCheckIntervalMs);
                    }
                };
                return client;
            }

            @Override
            public void close() {
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

    /**
     * Get a double from the random number generator.
     *
     * @return Randomly generated double.
     */
    private double nextDouble() {
        return tlr.get().nextDouble();
    }

    /**
     * Get an integer from the random number generator.
     * <P>
     * The number returned will range between 0 and the parameter (exclusive).
     *
     * @param i The upper bound (exclusive)
     * @return Randomly generated integer.
     */
    private int nextInt(int i) {
        return tlr.get().nextInt(i);
    }
}
