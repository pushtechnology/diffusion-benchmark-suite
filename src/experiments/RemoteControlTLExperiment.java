package experiments;

import static util.PropertiesUtil.getProperty;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import monitoring.Histogram;
import rc.BaseRemoteListener;
import rc.BaseService;
import util.Factory;
import clients.ExperimentClient;
import clients.LatencyMonitoringClient;

import com.pushtechnology.diffusion.api.APIException;
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
public final class RemoteControlTLExperiment implements Runnable {
    private static final SimpleTopicSpecification TOPIC_SPECIFICATION =
            new SimpleTopicSpecification();
    /**
     * client connections to be closed on close of factory and queried for
     * latency stats.
     */
    private final Set<LatencyMonitoringClient> clients =
            Collections
                    .newSetFromMap(new ConcurrentHashMap<LatencyMonitoringClient, Boolean>());

    private final class Service extends BaseService {
        public void startService() throws APIException {
            listener = new BaseRemoteListener(this);

            this.serverDetails =
                    ConnectionFactory.
                            createServerDetails(settings.getRcUrl());
            service = RemoteServiceFactory.
                    createRemoteService(serverDetails,
                            getControlTopicName(), getDomainTopicName(),
                            listener);

            listener.resetRegisterLatch();
            register();
            for (int i = 0; i < settings.getInitialTopics(); i++) {
                String topic = String.valueOf(i);
                service.addTopic(topic, TOPIC_SPECIFICATION);
                TopicMessage initialLoad = service.createLoadMessage(topic, 20);
                initialLoad.put("INIT");
                service.publish(initialLoad);
            }
            Runnable publishCommand = new Runnable() {
                private final byte[] message =
                        new byte[settings.getMessageSize()];
                private final ByteBuffer messageBuffer =
                        ByteBuffer.wrap(message);
                private final boolean shouldIncTopics =
                        settings.getTopicIncrementIntervalInPauses() != 0;
                private final boolean shouldIncMessages =
                        settings.getMessageIncrementIntervalInPauses() != 0;
                private int topics = settings.getInitialTopics();
                private int messages = settings.getInitialMessages();
                private int pubPauseCounter = 0;

                @Override
                public void run() {
                    pubPauseCounter++;
                    if(!service.isRegistered()){
                        try {
                            register();
                        } catch (APIException e) {
                            System.exit(-1);
                        }
                    }
                    for (int j = 0; j < messages; j++) {
                        for (int i = 0; i < topics; i++) {
                            publishToTopic(i);
                        }
                    }
                    if (shouldIncTopics && getTopInc() == 0) {
                        incTopics();
                    }
                    if (shouldIncMessages && getMessInc() == 0) {
                        messages += settings.getMessageIncrement();
                    }
                }

                public int getTopInc() {
                    return pubPauseCounter
                            % settings.getTopicIncrementIntervalInPauses();
                }

                public int getMessInc() {
                    return pubPauseCounter
                            % settings.getMessageIncrementIntervalInPauses();
                }

                public void incTopics() {
                    int targetTopics = topics + settings.getTopicIncrement();
                    for (int i = topics; i < targetTopics; i++) {
                        String topic = String.valueOf(i);
                        try {
                            service.addTopic(topic, TOPIC_SPECIFICATION);
                            TopicMessage initialLoad =
                                    service.createLoadMessage(topic, 20);
                            initialLoad.put("INIT");
                            service.publish(initialLoad);
                        } catch (APIException e) {
                            if (service.isRegistered()) {
                                Logs.warning("Failed to add topic", e);
                            }
                        }
                    }
                    topics = targetTopics;
                }

                public void publishToTopic(int i) {
                    try {
                        String topic = String.valueOf(i);
                        TopicMessage delta =
                                service.createDeltaMessage(topic,
                                        settings.getMessageSize());
                        messageBuffer.putLong(0, System.nanoTime());
                        delta.put(message);
                        service.publish(delta);
                    } catch (APIException ex) {
                        if (service.isRegistered()) {
                            Logs.warning("Failed to send delta", ex);
                        }
                    }
                }
            };
            Executors.newSingleThreadScheduledExecutor().
                    scheduleAtFixedRate(publishCommand, 0L,
                            settings.intervalPauseNanos, TimeUnit.NANOSECONDS);
        }

        public void register() throws APIException {
            service.getOptions().setAuthoriseSubscribeClients(false);
            service.getOptions().setClientConnectNotifications(false);
            service.getOptions().setRouteSelectorSubscribes(false);
            service.setMessageQueueSize(10000);
            service.getServerDetails().setOutputBufferSize(64*1024);
            service.getServerDetails().setInputBufferSize(64*1024);
            listener.resetRegisterLatch();
            service.register();
            while(!service.isRegistered()){
                listener.waitForRegistration(1000L, TimeUnit.MILLISECONDS);
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
        private final long intervalPauseNanos;

        private final int initialMessages;
        private final int messageIncrementIntervalInPauses;
        private final int messageIncrement;

        private final int initialTopics;
        private final int topicIncrementIntervalInPauses;
        private final int topicIncrement;

        private final String rcUrl;

        public Settings(Properties settings) {
            super(settings);
            intervalPauseNanos = (long) (getProperty(settings,
                    "publish.pause.interval.seconds", 0.25) * 1000000000L);
            initialMessages =
                    getProperty(settings, "publish.message.initial.count", 1);
            messageIncrementIntervalInPauses =
                    getProperty(settings, "publish.message.increment.pauses", 1);
            messageIncrement =
                    getProperty(settings, "publish.message.increment", 1);

            initialTopics =
                    getProperty(settings, "publish.topics.initial.count", 1);
            topicIncrementIntervalInPauses =
                    getProperty(settings, "publish.topics.increment.pauses", 1);
            topicIncrement =
                    getProperty(settings, "publish.topics.increment", 1);
            rcUrl =
                    getProperty(settings, "rc.host", "dpt://localhost:8080");
        }

        public long getIntervalPauseNanos() {
            return intervalPauseNanos;
        }

        public int getInitialMessages() {
            return initialMessages;
        }

        public int getMessageIncrementIntervalInPauses() {
            return messageIncrementIntervalInPauses;
        }

        public int getMessageIncrement() {
            return messageIncrement;
        }

        public int getInitialTopics() {
            return initialTopics;
        }

        public int getTopicIncrementIntervalInPauses() {
            return topicIncrementIntervalInPauses;
        }

        public int getTopicIncrement() {
            return topicIncrement;
        }

        // CHECKSTYLE:ON

        public String getRcUrl() {
            return rcUrl;
        }
    }

    /** the experiment loop. */
    private final ExperimentControlLoop loop;

    private final Settings settings;

    /**
     * @param settingsP ...
     */
    public RemoteControlTLExperiment(Settings settingsP) {
        this.settings = settingsP;
        loop = new ExperimentControlLoop(settingsP) {
            @Override
            protected void postInitialLoadCreated() {
                setUpRC();
            }

            @Override
            protected void wrapupAndReport() {
                // CHECKSTYLE:OFF
                Histogram histogramSummary = new Histogram(1024, 10000);
                // CHECKSTYLE:ON
                for (LatencyMonitoringClient connection : clients) {
                    histogramSummary.addObservations(connection.getHistogram());
                }
                getOutput().println(histogramSummary.toString());
            }
        };
        loop.setClientFactory(new Factory<ExperimentClient>() {
            @Override
            public ExperimentClient create() {
                LatencyMonitoringClient pingClient =
                        new LatencyMonitoringClient(loop
                                .getExperimentCounters(),
                                false, "DOMAIN//");
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

    public void setUpRC() {
        Service service = new Service();
        try {
            service.startService();
        } catch (APIException e) {
            new RuntimeException(e);
        }
    }

}
