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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.api.topic.Topic;

public class BroadcastRunner implements Runnable {
    public static final long PARK_NANOS_OVERHEAD = 55000;
    public static final long PARK_NANOS_THRESHOLD = 100000;
    private static final Logger LOG = LoggerFactory.getLogger(BroadcastRunner.class);

    private volatile boolean running = true;
    private final MessagePublisher messagePublisher;
    private final AtomicLong messageCounter;
    private final AtomicLong topicsCounter;
    private final BroadcastConfiguration config;

    public BroadcastRunner(final MessagePublisher messagePublisher,
            final AtomicLong messageCounter, final AtomicLong topicsCounter,
            final BroadcastConfiguration config) {
        this.messagePublisher = messagePublisher;
        this.messageCounter = messageCounter;
        this.topicsCounter = topicsCounter;
        this.config = config;
    }

    public void run() {
        running = true;
        
        final byte[] data = new byte[config.getMessageSize()];
        int pauseCounter = 0;
        for (int i = 0; i < config.getInitialTopicNum(); i++) {
            messagePublisher.addChildTopic(String.valueOf(i), data);
            topicsCounter.incrementAndGet();
        }
        int topicCounter = config.getInitialTopicNum();

        int messagesPerIntervalPerTopic = config
                .getMessagesPerIntervalPerTopic();
        while (running) {
            final long timestampNanos = System.nanoTime();
            if (messagePublisher.hasClients()) {
                for (int i = 0; i < messagesPerIntervalPerTopic; i++) {
                    for (Topic topic : messagePublisher.getChildTopics()) {
                        
                        try {
                            topic.lock();
                            messagePublisher.publish(topic, data);

                            messageCounter.lazySet(messageCounter.get() + 1);
                        } catch (Exception e) {
                            LOG.error("Runtime exception while publishing:",
                                    e);
                        } finally {
                            topic.unlock();
                        }
                    }
                }
            }

            boolean isCoping = applyInterval(timestampNanos);
            pauseCounter++;

            if (isCoping && messagePublisher.hasClients()) {
                // increase number of topics
                if (config.getTopicIncrementIntervalInPauses() != 0 &&
                        pauseCounter % config.getTopicIncrementIntervalInPauses() == 0) {
                    for (int i = 0; i < config.getTopicIncrement(); i++) {
                        messagePublisher.addChildTopic(
                                String.valueOf(topicCounter++), data);
                        topicsCounter.incrementAndGet();
                    }
                }
                // increase number of messages
                if (config.getMessageIncrementIntervalInPauses() != 0 &&
                        pauseCounter % config.getMessageIncrementIntervalInPauses() == 0) {
                    messagesPerIntervalPerTopic += config.getMessageIncrement();
                }
            }
        }
    }

    public void halt() {
        running = false;
    }

    private boolean applyInterval(final long startTimeNanos) {
        final long timeExpendedNanos = System.nanoTime() - startTimeNanos;
        long remainingInterval = config.getIntervalPauseNanos()
                - timeExpendedNanos;
        if (remainingInterval < 0) {
            return false;
        }

        if (remainingInterval < PARK_NANOS_THRESHOLD) {
            final long deadlineNanos = startTimeNanos
                    + config.getIntervalPauseNanos();
            while (System.nanoTime() < deadlineNanos) {
                // busy spin
            }
        } else {
            LockSupport.parkNanos(remainingInterval - PARK_NANOS_OVERHEAD);
        }
        return true;
    }
}
