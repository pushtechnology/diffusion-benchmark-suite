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
package clients;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;

import publishers.InjectionPublisher;
import util.ExperimentMonitor;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.APIProperties;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.PropertyException;
import com.pushtechnology.diffusion.api.ServerConnectionListener;
import com.pushtechnology.diffusion.api.threads.ThreadService;


/**
 * @author nwakart
 *
 */
public class DiffusionBenchmark {
    public static final String[] CONNECT_STRINGS;
    public static final int MAX_CLIENTS;
    public static final long CLIENT_CREATE_PAUSE_NANOS;
    public static final int INBOUND_THREAD_POOL_MAX_SIZE;
    public static final int INBOUND_THREAD_POOL_CORE_SIZE;
    public static final String[] NETWORK_INTERFACE_LIST;
    public static final long MAX_TEST_TIME;
    public static final int INITIAL_CLIENTS;
    public static final int CLIENT_INCREMENT;
    public static final String ROOT_TOPIC;
    public static final int CLIENT_INCREMENT_PAUSE_SECONDS;

    // init the benchmark setting from system properties
    static {
        CONNECT_STRINGS = System.getProperty("connect.string",
            "ws://eg12ph02:8080").split(",");

        MAX_CLIENTS = Integer.parseInt(System
            .getProperty("max.clients", "2000"));
        INITIAL_CLIENTS = Integer.parseInt(System
            .getProperty("initial.clients", "0"));
        CLIENT_INCREMENT = Integer.parseInt(System
            .getProperty("clients.increment", "50"));
        CLIENT_CREATE_PAUSE_NANOS =
            (long) (1000000000L * Double.parseDouble(System.getProperty(
                "client.create.pause.seconds", "0.001")));
        CLIENT_INCREMENT_PAUSE_SECONDS =
            Integer.parseInt(System.getProperty(
                "client.increment.pause.seconds", "1"));
        INBOUND_THREAD_POOL_MAX_SIZE = Integer.parseInt(System.getProperty(
            "inbound.threadpool.max.size", "1"));
        INBOUND_THREAD_POOL_CORE_SIZE = Integer.parseInt(System.getProperty(
            "inbound.threadpool.core.size", "1"));
        String locals = System.getProperty("local.interfaces", null);
        if(locals == null){
            NETWORK_INTERFACE_LIST = new String[]{};
        }
        else{
            NETWORK_INTERFACE_LIST = locals.split(",");
        }
        ROOT_TOPIC =
            System
                .getProperty("topic", InjectionPublisher.INJECTOR_ROOT + "//");
        MAX_TEST_TIME =
            (long) (1000L * 60L * Double.parseDouble(System.getProperty(
                "max.test.time.minutes", "5")));
    }

    Thread monitorThread;
    ExperimentMonitor benchmarkMonitor;
    final AtomicLong clientConnectCounter = new AtomicLong();
    final AtomicLong clientDisconnectCounter = new AtomicLong();
    final AtomicLong connectionRefusedCounter = new AtomicLong();
    final AtomicLong connectionAttemptsCounter = new AtomicLong();
    final AtomicLong messageCounter = new AtomicLong();
    final AtomicLong bytesCounter = new AtomicLong();
    final AtomicLong topicsCounter = new AtomicLong();

    final ServerConnectionListener topicsCountingCallback =
        new TopicCountingConnectionCallback(messageCounter,
            bytesCounter,
            clientConnectCounter,
            clientDisconnectCounter,
            connectionRefusedCounter,
            topicsCounter);

    final MessageCountingConnectionCallback messageCountingCallback =
        new MessageCountingConnectionCallback(
            messageCounter, bytesCounter,
            clientConnectCounter, clientDisconnectCounter,
            connectionRefusedCounter, false);

    final ClientConnectionFactory connector =
        new ClientConnectionFactory(connectionAttemptsCounter,
            connectionRefusedCounter, CONNECT_STRINGS,
            NETWORK_INTERFACE_LIST);

    protected static final Executor exec = Executors.newSingleThreadExecutor();

    public static void main(final String[] args) throws Exception {
        new DiffusionBenchmark().benchmark();
        System.exit(0);
    }

    public void benchmark() throws Exception {
        init();
        runBenchmark();
        wrapup();
    }

    protected void runBenchmark() {
        long testStartTime = System.currentTimeMillis();

        createFirstClient();
        // LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));
        Runnable createClientTask = new Runnable() {
            @Override
            public void run() {

                connectNewClient();
                if (CLIENT_CREATE_PAUSE_NANOS > 0L)
                    LockSupport.parkNanos(CLIENT_CREATE_PAUSE_NANOS);
            }
        };
        if (INITIAL_CLIENTS > 1) {
            for (int i = 0;i < INITIAL_CLIENTS - 1;i++) {
                exec.execute(createClientTask);
            }
        }
        postInitialLoadCreated();
        benchmarkMonitor.startSampling();
        int secondsSinceIncrement = 0;
        while (testNotOver(testStartTime)) {

            if (shouldIncrementLoad(secondsSinceIncrement)) {
                secondsSinceIncrement = 0;
                for (int i = 0;i < CLIENT_INCREMENT &&
                    getNumberCurrentlyConnected() < MAX_CLIENTS;i++) {
                    exec.execute(createClientTask);
                }
            }
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            secondsSinceIncrement++;
        }
    }

    protected void createFirstClient() {
        // note the topics counting client reconnects
        connector.createConnection(topicsCountingCallback, ROOT_TOPIC);
    }

    protected boolean shouldIncrementLoad(int secondsSinceIncrement) {
        return secondsSinceIncrement >= CLIENT_INCREMENT_PAUSE_SECONDS;
    }

    protected boolean testNotOver(long testStartTime) {
        return System.currentTimeMillis() - testStartTime < MAX_TEST_TIME;
    }

    protected void postInitialLoadCreated() {
    }

    protected long getNumberCurrentlyConnected() {
        return clientConnectCounter.get() - clientDisconnectCounter.get();
    }

    protected void wrapup() throws InterruptedException {
        benchmarkMonitor.halt();
        monitorThread.join();
    }

    protected void init() throws PropertyException, APIException {
        System.setProperty("diffusion.use.external.data", "true");
        Logs.setLevel(Level.OFF);
        APIProperties.setInboundThreadPoolQueueSize(MAX_CLIENTS * 2);
        ThreadService.getInboundThreadPool().setCoreSize(
            INBOUND_THREAD_POOL_CORE_SIZE);
        ThreadService.getInboundThreadPool().setMaximumSize(
            INBOUND_THREAD_POOL_MAX_SIZE);
        benchmarkMonitor =
            new ExperimentMonitor(
                messageCounter, bytesCounter,
                clientConnectCounter, clientDisconnectCounter,
                connectionAttemptsCounter, connectionRefusedCounter,
                topicsCounter);
        monitorThread = new Thread(benchmarkMonitor);
        monitorThread.setDaemon(true);
        monitorThread.setName("benchmark-monitor-thread");
        monitorThread.start();
    }

    protected ServerConnectionListener getConnectionListener() {
        return messageCountingCallback;
    }

    protected void connectNewClient() {
        connector.createConnection(getConnectionListener(), ROOT_TOPIC);
    }
}
