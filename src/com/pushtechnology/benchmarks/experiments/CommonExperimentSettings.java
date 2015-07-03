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

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A common set of settings for the client side of experiments. To be extended
 * should further settings be required.
 * 
 * @author nitsanw
 * 
 */
public class CommonExperimentSettings {
    // CHECKSTYLE:OFF adding docs will add nothing...
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_URL = "ws://localhost:8080";
    private static final int DEFAULT_INBOUND_THREAD_POOL_SIZE = 1;
    private static final int DEFAULT_CLIENT_INCREMENT_PAUSE_SECS = 5;
    private static final double DEFAULT_CLIENT_CREATE_PAUSE_SECS = 0.001;
    private static final int DEFAULT_CLIENT_INCREMENT = 50;
    private static final int DEFAULT_INITIAL_CLIENTS = 50;
    private static final int DEFAULT_MAX_CLIENTS = 175;
    private static final int DEFAULT_MESSAGE_SIZE = 128;
    private static final String DEFAULT_CONNECT_TOPIC_SELECTOR = "ROOT//";
    private static final double DEFAULT_MAX_TEST_TIME_MINUTES = 5.0;
    private static final Logger LOG = LoggerFactory.getLogger(CommonExperimentSettings.class);
    private static final long DEFAULT_WARMUP_MESSAGES = 20000;
    private final String[] diffusionUrls;
    private final int maxClients;
    private final long clientCreatePauseNanos;
    private final int inboundThreadPoolSize;
    private final String[] localInterfaces;
    private final int initialClients;
    private final int clientIncrement;
    private final String connectTopicSelector;
    private final int clientIncrementPauseSeconds;
    private final int messageSize;
    private final long maxTestTimeMillis;
    private final long maxTestMessages;
    private final long maxTestConnections;
    private final String outputFileName;
    private final String diffusionHost;
	private final long warmupMessages;
    // CHECKSTYLE:ON
    /**
     * Load the experiment settings from properties. Will modify the settings
     * to defaults used where no value is available.
     * 
     * @param settings ...
     */
    public CommonExperimentSettings(Properties settings) {
        diffusionUrls =
                getProperty(settings, "diffusion.url", DEFAULT_URL).split(",");
        diffusionHost = getProperty(settings, "diffusion.host", DEFAULT_HOST);
        maxClients = getProperty(settings, "max.clients", 
                DEFAULT_MAX_CLIENTS);
        initialClients = getProperty(settings, "initial.clients",
                DEFAULT_INITIAL_CLIENTS);
        clientIncrement = getProperty(settings, "clients.increment",
                DEFAULT_CLIENT_INCREMENT);
        clientCreatePauseNanos =
                (long) (TimeUnit.SECONDS.toNanos(1)
                * getProperty(settings,
                        "client.create.pause.seconds",
                        DEFAULT_CLIENT_CREATE_PAUSE_SECS));
        clientIncrementPauseSeconds =
                getProperty(settings, "client.increment.pause.seconds",
                        DEFAULT_CLIENT_INCREMENT_PAUSE_SECS);
        inboundThreadPoolSize =
                getProperty(settings, "inbound.threadpool.size",
                        DEFAULT_INBOUND_THREAD_POOL_SIZE);
        String localsInterfaces =
                getProperty(settings, "local.interfaces", "");
        if (localsInterfaces == null || localsInterfaces.isEmpty()) {
            localInterfaces = new String[] {};
        } else {
            localInterfaces = localsInterfaces.split(",");
        }
        connectTopicSelector =
                getProperty(settings, "topic",
                        DEFAULT_CONNECT_TOPIC_SELECTOR);
        messageSize = getProperty(settings, "message.size",
                DEFAULT_MESSAGE_SIZE);
        maxTestTimeMillis =
                (long) (TimeUnit.MINUTES.toMillis(1)
                 * getProperty(settings, "max.test.time.minutes",
                        DEFAULT_MAX_TEST_TIME_MINUTES));
        maxTestMessages = getProperty(settings, "max.test.messages", 0L);
        maxTestConnections = getProperty(settings, 
                "max.test.connections", 0L);
        outputFileName = getProperty(settings, "experiment.output", "");

        warmupMessages = getProperty(settings, "warmup.messages", DEFAULT_WARMUP_MESSAGES);
        
        logSettings();
    }

    // CHECKSTYLE:OFF adding docs will add nothing...
    public String[] getDiffusionUrls() {
        return diffusionUrls;
    }

    public int getMaxClients() {
        return maxClients;
    }

    public long getClientCreatePauseNanos() {
        return clientCreatePauseNanos;
    }

    public int getInboundThreadPoolSize() {
        return inboundThreadPoolSize;
    }

    public String[] getLocalInterfaces() {
        return localInterfaces;
    }

    public long getMaxTestTimeMillis() {
        return maxTestTimeMillis;
    }

    public int getInitialClients() {
        return initialClients;
    }

    public int getClientIncrement() {
        return clientIncrement;
    }

    public String getRootTopic() {
        return connectTopicSelector;
    }

    public int getClientIncrementPauseSeconds() {
        return clientIncrementPauseSeconds;
    }

    public int getMessageSize() {
        return messageSize;
    }

    public long getMaxTestMessages() {
        return maxTestMessages;
    }

    public long getMaxTestConnections() {
        return maxTestConnections;
    }
    public String getOutputFile() {
        return outputFileName;
    }

    public String getDiffusionHost() {
        return diffusionHost;
    }

    public String settingsAsString() {
        final StringBuilder builder = new StringBuilder(300);
        builder.append("Diffusion URLS: ");
        builder.append(this.getDiffusionUrls());
        builder.append('\n');
        builder.append("Max clients: ");
        builder.append(this.getMaxClients());
        builder.append('\n');
        builder.append("Client create pause nanos:      ");
        builder.append(this.getClientCreatePauseNanos());
        builder.append('\n');
        builder.append("Inbound thread pool size:       ");
        builder.append(this.getInboundThreadPoolSize());
        builder.append('\n');
        builder.append("Local interfaces:               ");
        builder.append(this.getLocalInterfaces());
        builder.append('\n');
        builder.append("Max test time millis:           ");
        builder.append(this.getMaxTestTimeMillis());
        builder.append('\n');
        builder.append("Initial clients:                ");
        builder.append(this.getInitialClients());
        builder.append('\n');
        builder.append("Client increment:               ");
        builder.append(this.getClientIncrement());
        builder.append('\n');
        builder.append("Root topic:                     ");
        builder.append(this.getRootTopic());
        builder.append('\n');
        builder.append("Client increment pause seconds: ");
        builder.append(this.getClientIncrementPauseSeconds());
        builder.append('\n');
        builder.append("Message size:                   ");
        builder.append(this.getMessageSize());
        builder.append('\n');
        builder.append("Max test messages:              ");
        builder.append(this.getMaxTestMessages());
        builder.append('\n');
        builder.append("Max test connections:           ");
        builder.append(this.getMaxTestConnections());
        builder.append('\n');
        builder.append("Output file:                    ");
        builder.append(this.getOutputFile());
        builder.append('\n');
        builder.append("Diffusion host:                 ");
        builder.append(this.getDiffusionHost());
        builder.append('\n');
        builder.append("Warmup Messages:                 ");
        builder.append(this.getWarmupMessages());
        builder.append('\n');
        
        return builder.toString();
    }

    public final void logSettings() {
        if (LOG.isTraceEnabled()) {
            LOG.trace(settingsAsString());
        }
    }

	public long getWarmupMessages() {
		return this.warmupMessages;
	}

	public boolean isPingTopicSend() {
		// TODO Auto-generated method stub
		return false;
	}
}
