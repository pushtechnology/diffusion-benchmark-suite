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

import java.util.concurrent.TimeUnit;

import util.PropertiesUtil;

/**
 * A common set of settings for the client side of experiments. To be extended
 * should further settings be required.
 * 
 * @author nitsanw
 * 
 */
public class CommonClientSettings {
    // CHECKSTYLE:OFF adding docs will add nothing...
    private static final int DEFAULT_INBOUND_THREAD_POOL_CORE_SIZE = 1;
    private static final int DEFAULT_INBOUND_THREAD_POOL_MAX_SIZE = 1;
    private static final int DEFAULT_CLIENT_INCREMENT_PAUSE_SECS = 5;
    private static final double DEFAULT_CLIENT_CREATE_PAUSE_SECS = 0.001;
    private static final int DEFAULT_CLIENT_INCREMENT = 50;
    private static final int DEFAULT_INITIAL_CLIENTS = 50;
    private static final int DEFAULT_MAX_CLIENTS = 175;
    private static final String DEFAULT_CONNECT_TOPIC_SELECTOR = "ROOT//";
    private static final double DEFAULT_MAX_TEST_TIME_MINUTES = 5.0;

    private final String[] diffusionUrls;
    private final int maxClients;
    private final long clientCreatePauseNanos;
    private final int inboundThreadPoolMaxSize;
    private final int inboundThreadPoolCoreSize;
    private final String[] localInterfaces;
    private final long maxTestTimeMillis;
    private final int initialClients;
    private final int clientIncrement;
    private final String connectTopicSelector;
    private final int clientIncrementPauseSeconds;
    // CHECKSTYLE:ON
    /**
     * Load the experiment setting from system properties.
     * 
     */
    public CommonClientSettings() {
        diffusionUrls =
                System.getProperty("connect.string", "ws://localhost:8080")
                        .split(",");

        maxClients = Integer.getInteger("max.clients", DEFAULT_MAX_CLIENTS);
        initialClients = Integer.getInteger("initial.clients", 
                DEFAULT_INITIAL_CLIENTS);
        clientIncrement = Integer.getInteger("clients.increment", 
                DEFAULT_CLIENT_INCREMENT);
        clientCreatePauseNanos =
                (long) (TimeUnit.SECONDS.toNanos(1)
                * PropertiesUtil.getSysPropertyVal(
                        "client.create.pause.seconds", 
                        DEFAULT_CLIENT_CREATE_PAUSE_SECS));
        clientIncrementPauseSeconds =
                Integer.getInteger("client.increment.pause.seconds",
                        DEFAULT_CLIENT_INCREMENT_PAUSE_SECS);
        inboundThreadPoolMaxSize =
                Integer.getInteger("inbound.threadpool.max.size", 
                        DEFAULT_INBOUND_THREAD_POOL_MAX_SIZE);
        inboundThreadPoolCoreSize =
                Integer.getInteger("inbound.threadpool.core.size", 
                        DEFAULT_INBOUND_THREAD_POOL_CORE_SIZE);
        String localsInterfaces = System.getProperty("local.interfaces", null);
        if (localsInterfaces == null || localsInterfaces.isEmpty()) {
            localInterfaces = new String[] {};
        } else {
            localInterfaces = localsInterfaces.split(",");
        }
        connectTopicSelector =
                System.getProperty("topic", DEFAULT_CONNECT_TOPIC_SELECTOR);
        maxTestTimeMillis =
                (long) (TimeUnit.MINUTES.toMillis(1) * PropertiesUtil
                        .getSysPropertyVal("max.test.time.minutes",
                                DEFAULT_MAX_TEST_TIME_MINUTES));
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

    public int getInboundThreadPoolMaxSize() {
        return inboundThreadPoolMaxSize;
    }

    public int getInboundThreadPoolCoreSize() {
        return inboundThreadPoolCoreSize;
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
    // CHECKSTYLE:ON
}
