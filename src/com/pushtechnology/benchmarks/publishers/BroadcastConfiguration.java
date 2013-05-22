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

public class BroadcastConfiguration {
    private final int messageSize;
    private final long intervalPauseNanos;

    private final int initialMessages;
    private final long messageIncrementIntervalInPauses;
    private final int messageIncrement;

    private final int initialTopics;
    private final long topicIncrementIntervalInPauses;
    private final int topicIncrementPerInterval;

    public BroadcastConfiguration(int messageSize, long intervalPauseNanos,
            int initialMessages, long messageIncrementInterval,
            int messageIncrementPerInterval, int initialTopicNum,
            long topicIncrementInterval, int topicIncrementPerInterval) {
        super();
        this.messageSize = messageSize;
        this.intervalPauseNanos = intervalPauseNanos;

        this.initialMessages = initialMessages;
        this.messageIncrementIntervalInPauses = messageIncrementInterval;
        this.messageIncrement = messageIncrementPerInterval;

        this.initialTopics = initialTopicNum;
        this.topicIncrementIntervalInPauses = topicIncrementInterval;
        this.topicIncrementPerInterval = topicIncrementPerInterval;
    }

    public long getIntervalPauseNanos() {
        return intervalPauseNanos;
    }

    public int getMessageSize() {
        return messageSize;
    }

    public int getMessagesPerIntervalPerTopic() {
        return initialMessages;
    }

    public long getTopicIncrementIntervalInPauses() {
        return topicIncrementIntervalInPauses;
    }

    public int getInitialTopicNum() {
        return initialTopics;
    }

    public int getTopicIncrement() {
        return topicIncrementPerInterval;
    }

    public long getMessageIncrementIntervalInPauses() {
        return messageIncrementIntervalInPauses;
    }

    public int getMessageIncrement() {
        return messageIncrement;
    }

}
