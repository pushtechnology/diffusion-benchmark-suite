package com.pushtechnology.benchmarks.experiments;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.benchmarks.clients.ExperimentClient;
import com.pushtechnology.benchmarks.clients.PingClient;
import com.pushtechnology.benchmarks.control.clients.BaseControlClient;
import com.pushtechnology.benchmarks.util.Factory;
import com.pushtechnology.benchmarks.util.PropertiesUtil;
import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.content.Content;
import com.pushtechnology.diffusion.client.features.RegisteredHandler;
import com.pushtechnology.diffusion.client.features.control.topics.MessagingControl;
import com.pushtechnology.diffusion.client.features.control.topics.MessagingControl.MessageHandler;
import com.pushtechnology.diffusion.client.features.control.topics.MessagingControl.SendCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicAddFailReason;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.AddCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.TopicSource;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.TopicSource.Updater;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.TopicSource.Updater.UpdateCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.TopicSource.Updater.UpdateError;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionClosedException;
import com.pushtechnology.diffusion.client.session.SessionId;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.client.types.ReceiveContext;

/**
 * Experiment to measure latency from control client.
 */
public final class ControlClientPingLatencyExperiment implements Runnable {
    /**
     * Log.
     */
    private static final Logger LOG =
        LoggerFactory.getLogger(ControlClientPingLatencyExperiment.class);
    /**
     * The ratio used by the histogram to scale its output.
     */
    private static final double HISTOGRAM_SCALING_RATIO = 1000.0;
    /**
     * The size of the input and output buffers.
     */
    private static final int BUFFER_SIZE = 64 * 1024;
    /**
     * Ping topic.
     */
    private static final String PING_TOPIC = "CC/Ping";
    /**
     * Control loop.
     */
    private final ExperimentControlLoop loop;
    
    /**
     * client connections to be closed on close of factory and queried for
     * latency stats.
     */
    private final Set<PingClient> clients = Collections
            .newSetFromMap(new ConcurrentHashMap<PingClient, Boolean>());

    /**
     * Constructor.
     * @param settings ..
     */
    public ControlClientPingLatencyExperiment(final Settings settings) {
        final ControlClient controlClient = new ControlClient(settings);
        loop = new ExperimentControlLoop(settings) {
            @Override
            protected void postInitialLoadCreated() {
                try {
                    controlClient.start();
                } catch (InterruptedException e) {
                    LOG.debug("Interrupted while waiting for control client");
                }
            }
            @Override
            protected void wrapupAndReport() {
                controlClient.stop();
                // CHECKSTYLE:OFF
                Histogram histogramSummary =
                        new Histogram(TimeUnit.SECONDS.toNanos(10), 3);

                for (PingClient connection : clients) {
                    histogramSummary.add(connection.getHistogram());
                }
                histogramSummary.getHistogramData().
                        outputPercentileDistribution(getOutput(), 1, HISTOGRAM_SCALING_RATIO);
                // CHECKSTYLE:ON
            }
        };
        loop.setClientFactory(new Factory<ExperimentClient>() {
            @Override
            public ExperimentClient create() {
                PingClient pingClient =
                        new PingClient(loop.getExperimentCounters(),
                                loop.getClientSettings().getMessageSize(),
                                PING_TOPIC);
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
        final ExperimentLoadStrategy defaultLoadStrategy =
            new DefaultLoadStrategy(loop.getClientSettings(),
                    loop.getExperimentCounters());
        loop.setLoadStartegy(defaultLoadStrategy);
    }

    @Override
    public void run() {
        loop.run();
    }

    /**
     * Experiment control client.
     */
    private static final class ControlClient extends BaseControlClient {
        /**
         * Constructor.
         * @param settings ..
         */
        private ControlClient(Settings settings) {
            super(settings.getControlClientURL(), BUFFER_SIZE, 2);
        }

        @Override
        public void initialise(final Session session) {
            final TopicUpdateControl updateControl =
                session.feature(TopicUpdateControl.class);

            updateControl.addTopicSource(PING_TOPIC, new TopicSource() {
                @Override
                public void onActive(String topicPath,
                        RegisteredHandler handler,
                        final Updater updater) {
                    final TopicControl topicControl =
                        session.feature(TopicControl.class);
                    createInitalTopic(topicControl, updater);
                }
                @Override
                public void onClosed(String topicPath) {
                }
                @Override
                public void onStandBy(String topicPath) {
                    LOG.warn("Failed to become source for {}", topicPath);
                }
            });

            final MessagingControl messagingControl =
                session.feature(MessagingControl.class);
            messagingControl.addMessageHandler(PING_TOPIC,
                new EchoHandler(messagingControl));
        }

        /**
         * Create ping topic.
         * @param topicControl ..
         * @param updater ..
         */
        private void createInitalTopic(TopicControl topicControl,
                final Updater updater) {
            topicControl.addTopic(PING_TOPIC, TopicType.STATELESS,
                    new AddCallback() {
                @Override
                public void onDiscard() {
                }
                @Override
                public void onTopicAddFailed(String topic,
                        TopicAddFailReason failure) {
                    LOG.warn("Failed to create topic {}", topic);
                }
                @Override
                public void onTopicAdded(String topic) {
                    updater.update(PING_TOPIC,
                            Diffusion.content().newContent("INIT"),
                            new UpdateCallback() {
                        @Override
                        public void onError(String topic, UpdateError error) {
                            LOG.warn("Failed to initialise {}", topic);
                        }
                        @Override
                        public void onSuccess(String topic) {
                            initialised();
                        }
                    });
                }
            });
        }

        /**
         * Echo any messages received on a topic.
         */
        private final class EchoHandler implements MessageHandler {
            /**
             * Messaging control.
             */
            private final MessagingControl messagingControl;
            /**
             * Constructor.
             * @param messaging ..
             */
            public EchoHandler(MessagingControl messaging) {
                this.messagingControl = messaging;
            }
            @Override
            public void onActive(String topicPath, RegisteredHandler handler) {
                initialised();
            }
            @Override
            public void onClose(String topicPath) {
            }
            @Override
            public void onMessage(SessionId session, String topic,
                    Content message, ReceiveContext context) {
                try {
                    messagingControl.send(session, PING_TOPIC, message,
                            new SendCallback() {
                        @Override
                        public void onDiscard() {
                        }
                        @Override
                        public void onComplete() {
                        }
                    });
                } catch (SessionClosedException e) {
                    LOG.debug("Session {} has closed", session);
                }
            }
        }
    }

    /**
     * Experiment specific settings.
     */
    public static final class Settings extends CommonExperimentSettings {
        /**
         * Control client URL.
         */
        private final String controlClientURL;

        /**
         * Constructor.
         * @param settings ..
         */
        public Settings(Properties settings) {
            super(settings);
            controlClientURL = PropertiesUtil.
                getProperty(settings, "cc.host", "dpt://localhost:8081");
        }

        /**
         * @return The URL for the control client to connect to.
         */
        public String getControlClientURL() {
            return controlClientURL;
        }
    }
}
