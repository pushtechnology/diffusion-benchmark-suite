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
package com.pushtechnology.benchmarks.publishers;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.TimeoutException;
import com.pushtechnology.diffusion.api.config.ConfigManager;
import com.pushtechnology.diffusion.api.config.ConflationPolicyConfig;
import com.pushtechnology.diffusion.api.config.ConflationPolicyConfig.Mode;
import com.pushtechnology.diffusion.api.data.TopicData;
import com.pushtechnology.diffusion.api.data.TopicDataType;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.message.TopicMessageComparator;
import com.pushtechnology.diffusion.api.message.TopicMessageComparators;
import com.pushtechnology.diffusion.api.publisher.Client;
import com.pushtechnology.diffusion.api.publisher.Publisher;
import com.pushtechnology.diffusion.api.topic.Topic;
import com.pushtechnology.diffusion.api.topic.TopicClient;
import com.pushtechnology.diffusion.api.topic.TopicLoader;
import com.pushtechnology.diffusion.api.topic.TopicTreeNode;
import com.pushtechnology.diffusion.data.TopicDataImpl;

public final class BroadcastPublisher extends Publisher implements
        MessagePublisher {
    public static final String INJECTOR_ROOT = "ROOT";

    private Topic rootTopic;
    private Queue<Topic> childTopics = new ConcurrentLinkedQueue<Topic>();
    private PublisherAssembly publisherAssembly;
    private boolean conflation = false;

    @Override
    protected void initialLoad() throws APIException {
        System.out.println(getPublisherName()+".initialLoad");
        TopicData data = new TopicDataImpl() {

            @Override
            public TopicDataType getType() {
                return TopicDataType.CUSTOM;
            }

            @Override
            public TopicMessage getLoadMessage(TopicClient client)
                    throws TimeoutException, APIException {
                return getLoadMessage();
            }

            @Override
            public TopicMessage getLoadMessage() throws TimeoutException,
                    APIException {
                final TopicMessage loadMessage = getTopic().createLoadMessage(
                        20);
                loadMessage.put("ROOT");
                return loadMessage;
            }

            @Override
            protected void attachedToTopic(String topicName,
                    TopicTreeNode parent) throws APIException {
            }
        };
        rootTopic = addTopic(INJECTOR_ROOT, data);
        rootTopic.setAutoSubscribe(true);
        String conflationMode = getProperty("conflationMode", "NONE");
        if (!conflationMode.equals("NONE")) {
            conflation = true;
            // Setting up conflation by topic, one message for topic
            if (conflationMode.equals("APPEND")) {
                setupDefaultPolicy(ConflationPolicyConfig.Mode.APPEND);
            } else if (conflationMode.equals("REPLACE")) {
                setupDefaultPolicy(ConflationPolicyConfig.Mode.REPLACE);
            } else if (conflationMode.equals("COMPARE")) {
                setupDefaultComparator();
            } else if (conflationMode.equals("MERGE")) {
                setupMergePolicy();
            } else {
                conflation = false;
            }
        }
        // message size is set per experiment
        int messageSize = getProperty("messageSize", 100);
        // the interval drives both publications and ramping changes
        long intervalPauseNanos = (long) (1000000000L * getProperty(
                "intervalPauseSeconds", 0.1));

        // message publications, all topics are updated
        int initialMessages = getProperty("initialMessages", 100);
        long messageIncrementIntervalInPauses = getProperty(
                "messageIncrementIntervalInPauses", 10L);
        int messageIncrement = getProperty("messageIncrement", 100);

        // topics
        int initialTopicNum = getProperty("initialTopicNum", 100);
        long topicIncrementIntervalInPauses = getProperty(
                "topicIncrementIntervalInPauses", 10);
        int topicIncrement = getProperty("topicIncrement", 10);
        BroadcastConfiguration config = new BroadcastConfiguration(messageSize,
                intervalPauseNanos, initialMessages,
                messageIncrementIntervalInPauses, messageIncrement,
                initialTopicNum, topicIncrementIntervalInPauses, topicIncrement);
        publisherAssembly = new PublisherAssembly(this, config);
    }

    protected static void setupMergePolicy() throws APIException {
        setupDefaultPolicy(ConflationPolicyConfig.Mode.REPLACE);
    }

    protected static void setupDefaultPolicy(Mode mode) throws APIException {
        ConflationPolicyConfig policy = ConfigManager.getConfig()
                .getConflation().addPolicy("XXX", mode);
        ConfigManager.getConfig().getConflation()
                .setDefaultPolicy(policy.getName());
    }

    protected static void setupDefaultComparator() {
        TopicMessageComparators
                .setDefaultComparator(new TopicMessageComparator() {

                    @Override
                    protected boolean equalsTopicMessage(TopicMessage message1,
                            TopicMessage message2) {
                        return message1.getTopicName().equals(
                                message2.getTopicName());
                    }
                });
    }

    private double getProperty(String prop, double defaultVal) {
        try {
            return Double.valueOf(getProperty(prop));
        } catch (Exception e) {

        }
        return defaultVal;
    }

    private String getProperty(String prop, String defaultVal) {
        if (getProperty(prop) != null) {
            return getProperty(prop);
        }
        return defaultVal;
    }

    private int getProperty(String prop, int defaultVal) {
        try {
            return getIntegerProperty(prop);
        } catch (Exception e) {

        }
        return defaultVal;
    }

    private long getProperty(String prop, long defaultVal) {
        try {
            return Long.valueOf(getProperty(prop));
        } catch (Exception e) {

        }
        return defaultVal;
    }

    @Override
    protected void publisherStarted() throws APIException {
        System.out.println("InjectionPublisher.publisherStarted");
        publisherAssembly.init();
    }

    @Override
    protected void publisherStopping() throws APIException {
        System.out.println(getPublisherName()+".publisherStopping");
        publisherAssembly.destroy();
        childTopics.clear();
    }

    @Override
    protected void publisherStopped() throws APIException {
        System.out.println(getPublisherName()+".publisherStopped");
        super.publisherStopped();
    }

    @Override
    protected boolean isStoppable() {
        return true;
    }

    @Override
    protected void subscription(final Client client, final Topic topic,
            final boolean loaded) throws APIException {
        if (topic.equals(rootTopic)) {
            client.setConflation(conflation);
            for (Topic childTopic : childTopics) {
                client.subscribe(childTopic, true);
            }
        }
    }

    @Override
    public void publish(final Topic topic, final String... message) {
        try {
            final TopicMessage deltaMessage = topic.createDeltaMessage();
            deltaMessage.putFields(message);
            topic.publishMessage(deltaMessage);
        } catch (APIException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addChildTopic(String topicName, final String loaddata) {
        try {
            TopicData data = new TopicDataImpl() {

                @Override
                public TopicDataType getType() {
                    return TopicDataType.CUSTOM;
                }

                @Override
                public TopicMessage getLoadMessage(TopicClient client)
                        throws TimeoutException, APIException {
                    return getLoadMessage();
                }

                @Override
                public TopicMessage getLoadMessage() throws TimeoutException,
                        APIException {
                    final TopicMessage loadMessage = getTopic()
                            .createLoadMessage(loaddata.length() + 20);
                    loadMessage.put(loaddata);
                    return loadMessage;
                }

                @Override
                protected void attachedToTopic(String topicName,
                        TopicTreeNode parent) throws APIException {
                }
            };
            final Topic childTopic = addTopic(topicName, rootTopic, data);
            final TopicMessage loadMessage = childTopic
                    .createLoadMessage(loaddata.length() + 20);
            loadMessage.put(loaddata);
            addTopicLoader(new TopicLoader() {
                public boolean load(TopicClient client, Topic topic)
                        throws APIException {
                    client.send(loadMessage);
                    return true;
                }

            }, childTopic.getName());
            childTopics.add(childTopic);
            final List<TopicClient> clients = getClients(rootTopic);
            for (TopicClient client : clients) {
                client.subscribe(childTopic, true);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public final Queue<Topic> getChildTopics() {
        return childTopics;
    }

    @Override
    public boolean hasClients() {
        return rootTopic.hasSubscribers();
    }

}
