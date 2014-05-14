package com.pushtechnology.benchmarks.experiments;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.HdrHistogram.Histogram;

import com.pushtechnology.benchmarks.clients.ExperimentClient;
import com.pushtechnology.benchmarks.clients.PingClient;
import com.pushtechnology.benchmarks.control.clients.BaseControlClient;
import com.pushtechnology.benchmarks.util.Factory;
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
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.TopicSource.Updater.UpdateCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.TopicSource.Updater.UpdateError;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionId;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.client.types.ReceiveContext;

public class ControlClientPingLatencyExperiment implements Runnable {
    /**
     * The ratio used by the histogram to scale its output.
     */
    private static final double HISTOGRAM_SCALING_RATIO = 1000.0;
    protected static final String PING_TOPIC = "CC/Ping";
    private final ExperimentControlLoop loop;
    private final BaseControlClient controlClient;
    /**
     * client connections to be closed on close of factory and queried for
     * latency stats.
     */
    private final Set<PingClient> clients = Collections
            .newSetFromMap(new ConcurrentHashMap<PingClient, Boolean>());

    public ControlClientPingLatencyExperiment(final CommonExperimentSettings settings) {
        controlClient = new BaseControlClient(2) {
            @Override
            public void initialise(final Session session) {
                final TopicUpdateControl updateControl = session.feature(TopicUpdateControl.class);
                updateControl.addTopicSource(PING_TOPIC, new TopicSource() {
                    @Override
                    public void onActive(String topicPath, RegisteredHandler handler, final Updater updater) {
                        final TopicControl topicControl = session.feature(TopicControl.class);
                        topicControl.addTopic(PING_TOPIC, TopicType.STATELESS, new AddCallback() {
                            @Override
                            public void onDiscard() {
                            }
                            @Override
                            public void onTopicAddFailed(String topic, TopicAddFailReason failure) {
                            }
                            @Override
                            public void onTopicAdded(String topic) {
                                updater.update(PING_TOPIC, Diffusion.content().newContent("INIT"), new UpdateCallback() {
                                    @Override
                                    public void onError(String topic, UpdateError error) {
                                    }
                                    @Override
                                    public void onSuccess(String topic) {
                                        controlClient.initialised();
                                    }
                                });
                            }
                        });
                    }
                    @Override
                    public void onClosed(String topicPath) {
                    }
                    @Override
                    public void onStandBy(String topicPath) {
                    }
                });

                final MessagingControl messagingControl = session.feature(MessagingControl.class);
                messagingControl.addMessageHandler(PING_TOPIC, new MessageHandler() {
                    @Override
                    public void onActive(String topic, RegisteredHandler handler) {
                        controlClient.initialised();
                    }
                    @Override
                    public void onClose(String topic) {
                    }
                    @Override
                    public void onMessage(SessionId id, String topic, Content content, ReceiveContext context) {
                        messagingControl.send(id, PING_TOPIC, content, new SendCallback() {
                            @Override
                            public void onDiscard() {
                            }
                            @Override
                            public void onComplete() {
                            }
                        });
                    }
                });
            }
        };
        loop = new ExperimentControlLoop(settings) {
            @Override
            protected void postInitialLoadCreated() {
                    try {
                        controlClient.start();
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
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
    }

    @Override
    public void run() {
        loop.run();
    }
}
