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
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.TopicSource;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.TopicSource.Updater.UpdateCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.TopicSource.Updater.UpdateError;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

public class ControlClientTLExperiment implements Runnable {
    private static final double HISTOGRAM_SCALING_RATIO = 1000.0;
    /**
     * The size of the input and output buffers.
     */
    private static final int BUFFER_SIZE = 64 * 1024;

    private final Set<LatencyMonitoringClient> clients =
            Collections.newSetFromMap(
                    new ConcurrentHashMap<LatencyMonitoringClient, Boolean>());
    private final ExperimentControlLoop loop;

    /**
     * @param settingsP ...
     */
    public ControlClientTLExperiment(final Settings settings) {
        loop = new ExperimentControlLoop(settings) {
            @Override
            protected void postInitialLoadCreated() {
                final BaseControlClient controlClient = new BaseControlClient(settings.getControlClientUrl(), BUFFER_SIZE, 1) {
                    @Override
                    public void initialise(final Session session) {
                        final TopicUpdateControl updateControl = session.feature(TopicUpdateControl.class);
                        updateControl.addTopicSource("DOMAIN", new TopicSource() {
                            @Override
                            public void onActive(String arg0, RegisteredHandler arg1, final Updater updater) {
                                final TopicControl topicControl = session.feature(TopicControl.class);
                                for (int i = 0; i < settings.getInitialTopics(); i++) {
                                    addTopic(topicControl, updater, i);
                                }
                                Runnable publishCommand = new Runnable() {
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
                                            addTopic(topicControl, updater, i);
                                        }
                                        topics = targetTopics;
                                    }

                                    public void publishToTopic(int i) {
                                        final byte[] bytes = new byte[settings.getMessageSize()];
                                        final ByteBuffer buffer = ByteBuffer.wrap(bytes);
                                        buffer.putLong(System.nanoTime());
                                        final Content content = Diffusion.content().newContent(bytes);
                                        updater.update("DOMAIN/"+i, content, new UpdateCallback() {
                                            @Override
                                            public void onError(String arg0, UpdateError arg1) {
                                            }
                                            @Override
                                            public void onSuccess(String arg0) {
                                            }
                                        });
                                    }
                                };
                                Executors.newSingleThreadScheduledExecutor().
                                        scheduleAtFixedRate(publishCommand, 0L,
                                                settings.intervalPauseNanos, TimeUnit.NANOSECONDS);
                                initialised();
                            }

                            private void addTopic(TopicControl topicControl, final Updater updater, int i) {
                                final Content content = Diffusion.content().newContent("INIT");
                                topicControl.addTopic("DOMAIN/" + i, TopicType.STATELESS, new TopicControl.AddCallback() {
                                    @Override
                                    public void onDiscard() {
                                    }
                                    @Override
                                    public void onTopicAddFailed(String topic, TopicAddFailReason reason) {
                                    }
                                    @Override
                                    public void onTopicAdded(String topic) {
                                        updater.update(topic, content, new UpdateCallback() {
                                            @Override
                                            public void onError(String arg0, UpdateError arg1) {
                                            }
                                            @Override
                                            public void onSuccess(String arg0) {
                                            }
                                        });
                                    }
                                });
                            }
                            @Override
                            public void onClosed(String arg0) {
                            }
                            @Override
                            public void onStandBy(String arg0) {
                            }
                        });
                    }
                };
                try {
                    controlClient.start();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
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
            return rcUrl;
        }
    }
}