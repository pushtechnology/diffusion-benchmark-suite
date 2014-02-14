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

import static com.pushtechnology.benchmarks.util.PropertiesUtil.getProperty;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.HdrHistogram.Histogram;

import com.pushtechnology.benchmarks.clients.ExperimentClient;
import com.pushtechnology.benchmarks.clients.LatencyMonitoringClient;
import com.pushtechnology.benchmarks.clients.PingClient;
import com.pushtechnology.benchmarks.rc.BaseRemoteListener;
import com.pushtechnology.benchmarks.rc.BaseService;
import com.pushtechnology.benchmarks.util.Factory;
import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.ClientDetails;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.connection.ConnectionFactory;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.remote.RemoteServiceFactory;
import com.pushtechnology.diffusion.api.remote.topics.SimpleTopicSpecification;

/**
 * Remote control base throughput and latency experiment.
 * 
 * @author nitsanw
 * 
 */
public final class RemoteControlPingLatencyExperiment implements Runnable {
    /**
     * The ratio used by the histogram to scale its output.
     */
    private static final double HISTOGRAM_SCALING_RATIO = 1000.0;
    /**
     * Topic specification for experiment.
     */
    private static final SimpleTopicSpecification TOPIC_SPECIFICATION =
            new SimpleTopicSpecification();
    /**
     * Number of milliseconds to wait for registration.
     */
    private static final long REGISTRATION_WAIT = 1000L;
    /**
     * The size of the input and output buffers.
     */
    private static final int BUFFER_SIZE = 64 * 1024;
    /**
     * The size of the message queue.
     */
    private static final int MESSAGE_QUEUE_SIZE = 10000;
    /**
     * The initial size of messages.
     */
    private static final int INITIAL_SIZE_OF_MESSAGES = 20;

    /**
     * client connections to be closed on close of factory and queried for
     * latency stats.
     */
    private final Set<LatencyMonitoringClient> clients =
        Collections.newSetFromMap(
            new ConcurrentHashMap<LatencyMonitoringClient, Boolean>());

    /**
     * The remote service to use in the experiment. 
     */
    private final class Service extends BaseService {
        /**
         * Start the service.
         * @throws APIException If unable to start the service.
         */
        public void startService() throws APIException {
            listener = new BaseRemoteListener(this) {
                @Override
                public void messageFromClient(ClientDetails clientDetails,
                        String topicName, TopicMessage message) {
                    try {
                        service.getRemoteService().sendToClient(
                                clientDetails.getClientID(), message);
                    } catch (APIException e) {
                        e.printStackTrace();
                    }
                }
            };

            this.serverDetails =
                    ConnectionFactory.
                            createServerDetails(settings.getRcUrl());
            service = RemoteServiceFactory.
                    createRemoteService(serverDetails,
                            getControlTopicName(), getDomainTopicName(),
                            listener);

            listener.resetRegisterLatch();
            register();
            service.addTopic("PING", TOPIC_SPECIFICATION);
            TopicMessage initialLoad =
                    service.createLoadMessage("PING", INITIAL_SIZE_OF_MESSAGES);
            initialLoad.put("INIT");
            service.publish(initialLoad);
        }

        /**
         * Register the service.
         *
         * @throws APIException If the service could not be registered.
         */
        @SuppressWarnings("deprecation")
        public void register() throws APIException {
            service.getOptions().setAuthoriseSubscribeClients(false);
            service.getOptions().setClientConnectNotifications(false);
            service.getOptions().setRouteSelectorSubscribes(false);
            service.setMessageQueueSize(MESSAGE_QUEUE_SIZE);
            service.getServerDetails().setOutputBufferSize(BUFFER_SIZE);
            service.getServerDetails().setInputBufferSize(BUFFER_SIZE);
            listener.resetRegisterLatch();
            service.register();
            while (!service.isRegistered()) {
                listener.waitForRegistration(REGISTRATION_WAIT,
                        TimeUnit.MILLISECONDS);
                Logs.info("Registering RC with server...");
            }
        }

        @Override
        public String getDomainTopicName() {
            return "DOMAIN";
        }
    }

    /** Experiment specialized settings. */
    public static class Settings extends CommonExperimentSettings {
        // CHECKSTYLE:OFF
        private final String rcUrl;

        public Settings(Properties settings) {
            super(settings);
            rcUrl = getProperty(settings, "rc.host", "dpt://localhost:8080");
        }

        public String getRcUrl() {
            return rcUrl;
        }
        // CHECKSTYLE:ON
    }

    /** the experiment loop. */
    private final ExperimentControlLoop loop;

    /** SETTINGS. */
    private final Settings settings;

    /**
     * @param settingsP ...
     */
    public RemoteControlPingLatencyExperiment(Settings settingsP) {
        this.settings = settingsP;
        loop = new ExperimentControlLoop(settingsP) {
            @Override
            protected void postInitialLoadCreated() {
                setUpRC();
            }

            @Override
            protected void wrapupAndReport() {
                // CHECKSTYLE:OFF
                Histogram histogramSummary =
                        new Histogram(TimeUnit.SECONDS.toNanos(10), 3);
                // CHECKSTYLE:ON
                for (LatencyMonitoringClient connection : clients) {
                    histogramSummary.add(connection.getHistogram());
                }
                histogramSummary.getHistogramData().
                    outputPercentileDistribution(
                            getOutput(),
                            1,
                            HISTOGRAM_SCALING_RATIO);
            }
        };
        loop.setClientFactory(new Factory<ExperimentClient>() {
            @Override
            public ExperimentClient create() {
                LatencyMonitoringClient pingClient =
                        new PingClient(loop
                                .getExperimentCounters(),
                                settings.getMessageSize(), "DOMAIN/PING");
                clients.add(pingClient);
                return pingClient;
            }

            @Override
            public void close() {
                for (LatencyMonitoringClient connection : clients) {
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

    /**
     * Set up and start the service for the experiment.
     */
    public void setUpRC() {
        Service service = new Service();
        try {
            service.startService();
        } catch (APIException e) {
            new RuntimeException(e);
        }
    }

}
