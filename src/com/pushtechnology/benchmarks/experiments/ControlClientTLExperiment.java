package com.pushtechnology.benchmarks.experiments;

import static com.pushtechnology.benchmarks.util.PropertiesUtil.getProperty;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.benchmarks.clients.ExperimentClient;
import com.pushtechnology.benchmarks.clients.LatencyMonitoringClient;
import com.pushtechnology.benchmarks.clients.UnsafeLatencyMonitoringClient;
import com.pushtechnology.benchmarks.control.clients.BaseControlClient;
import com.pushtechnology.benchmarks.util.Factory;
import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.content.Content;
import com.pushtechnology.diffusion.client.features.RegisteredHandler;
import com.pushtechnology.diffusion.client.features.control.topics.TopicAddFailReason;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.AddCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.TopicSource;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.TopicSource.Updater;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.TopicSource.Updater.UpdateCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.TopicSource.Updater.UpdateError;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * Experiment to measure throughput from control client.
 */
public final class ControlClientTLExperiment implements Runnable {
    /**
     * Log.
     */
    private static final Logger LOG =
        LoggerFactory.getLogger(ControlClientTLExperiment.class);
    /**
     * The ratio used by the histogram to scale its output.
     */
    private static final double HISTOGRAM_SCALING_RATIO = 1000.0;
    /**
     * The size of the input and output buffers.
     */
    private static final int BUFFER_SIZE = 64 * 1024;

    /**
     * The clients.
     */
    private final Set<LatencyMonitoringClient> clients =
            Collections.newSetFromMap(
                    new ConcurrentHashMap<LatencyMonitoringClient, Boolean>());
    /**
     * Control loop.
     */
    private final ExperimentControlLoop loop;

    /**
     * @param settings ...
     */
    public ControlClientTLExperiment(final Settings settings) {
        loop = new ExperimentControlLoop(settings) {
            @Override
            protected void postInitialLoadCreated() {
                final ControlClient controlClient = new ControlClient(settings);
                try {
                    controlClient.start();
                } catch (InterruptedException e) {
                    LOG.debug("Interrupted while waiting for control client");
                }
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
                        new UnsafeLatencyMonitoringClient(loop
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
     * The Control Client used by the experiment.
     */
    private static final class ControlClient extends BaseControlClient {
        /**
         * Initial message.
         */
        private static final Content INITIAL_CONTENT =
            Diffusion.content().newContent("INIT");
        /**
         * The settings.
         */
        private final Settings settings;
        /**
         * The topic control.
         */
        private TopicControl topicControl;
        /**
         * The update control.
         */
        private TopicUpdateControl updateControl;
        /**
         * The updater.
         */
        private Updater updater;
        /**
         * 
         */
        private AddCallback addTopicCallback;

        /**
         * Constructor.
         * @param settingsP ..
         */
        private ControlClient(Settings settingsP) {
            super(settingsP.getControlClientUrl(), BUFFER_SIZE, 1);
            settings = settingsP;

            addTopicCallback = new TopicControl.AddCallback() {
                @Override
                public void onDiscard() {
                }
                @Override
                public void onTopicAddFailed(String topic,
                        TopicAddFailReason reason) {
                    LOG.debug("Failed to add topic {}", topic);
                }
                @Override
                public void onTopicAdded(String topic) {
                    updater.update(topic, INITIAL_CONTENT,
                            new UpdateCallback() {
                        @Override
                        public void onError(String topic, UpdateError error) {
                            LOG.debug("Failed to update topic {}", topic);
                        }
                        @Override
                        public void onSuccess(String topic) {
                        }
                    });
                }
            };
        }

        @Override
        public void initialise(final Session session) {
            updateControl = session.feature(TopicUpdateControl.class);
            updateControl.addTopicSource("DOMAIN", new TopicSource() {
                @Override
                public void onActive(String topicPath,
                        RegisteredHandler handler, final Updater updaterP) {
                    updater = updaterP;
                    topicControl = session.feature(TopicControl.class);
                    for (int i = 0; i < settings.getInitialTopics(); i++) {
                        topicControl.addTopic("DOMAIN/" + i,
                            TopicType.STATELESS, addTopicCallback);
                    }

                    Executors.newSingleThreadScheduledExecutor()
                        .scheduleAtFixedRate(
                            new LoadTask(),
                            0L,
                            settings.intervalPauseNanos,
                            TimeUnit.NANOSECONDS);
                    initialised();
                }

                @Override
                public void onClosed(String topicPath) {
                }
                @Override
                public void onStandBy(String topicPath) {
                    LOG.warn("Failed to become source for {}", topicPath);
                }
            });
        }

        /**
         * Publish message to topic.
         *
         * @param i ..
         */
        public void publishToTopic(int i) {
            final byte[] bytes = new byte[settings.getMessageSize()];
            final ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.putLong(System.nanoTime());
            final Content content = Diffusion.content().newContent(bytes);
            updater.update("DOMAIN/" + i, content, new UpdateCallback() {
                @Override
                public void onError(String topic, UpdateError error) {
                    LOG.debug("Failed to update topic {}", topic);
                }
                @Override
                public void onSuccess(String topic) {
                }
            });
        }

        /**
         * Runnable to publish messages and increase number of topics.
         */
        private final class LoadTask implements Runnable {
            /**
             * Increment the topics.
             */
            private final boolean shouldIncTopics =
                settings.getTopicIncrementIntervalInPauses() != 0;
            /**
             * Increment the messages.
             */
            private final boolean shouldIncMessages =
                settings.getMessageIncrementIntervalInPauses() != 0;
            /**
             * Number of topics.
             */
            private int topics = settings.getInitialTopics();
            /**
             * Number of messages.
             */
            private int messages = settings.getInitialMessages();
            /**
             * Iterations counter.
             */
            private int pubPauseCounter = 0;

            @Override
            public void run() {
                pubPauseCounter++;
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

            /**
             * @return The topics to increment by.
             */
            public int getTopInc() {
                return pubPauseCounter
                    % settings.getTopicIncrementIntervalInPauses();
            }

            /**
             * @return The messages to increment by.
             */
            public int getMessInc() {
                return pubPauseCounter
                    % settings.getMessageIncrementIntervalInPauses();
            }

            /**
             * Increment the topics.
             */
            public void incTopics() {
                int targetTopics = topics + settings.getTopicIncrement();
                for (int i = topics; i < targetTopics; i++) {
                    topicControl.addTopic("DOMAIN/" + i, TopicType.STATELESS,
                        addTopicCallback);
                }
                topics = targetTopics;
            }
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

        private final String ccUrl;

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
            ccUrl =
                getProperty(settings, "cc.host", "dpt://localhost:8081");
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

        /**
         * Get the URL of the control client.
         * 
         * @return The URL for the control client.
         */
        public final String getControlClientUrl() {
            return ccUrl;
        }
    }
}
