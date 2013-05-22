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

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.data.TopicDataFactory;
import com.pushtechnology.diffusion.api.data.metadata.MDataType;
import com.pushtechnology.diffusion.api.data.single.SingleValueTopicData;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.publisher.Client;
import com.pushtechnology.diffusion.api.publisher.Publisher;
import com.pushtechnology.diffusion.api.topic.Topic;

/**
 * An echo/ping/repeater publisher for benchmarking client RTT.
 * 
 * @author nitsanw
 */
public final class PingPublisher extends Publisher {
    /** the ping topic. Messages sent here will be sent back. */
    public static final String ROOT_TOPIC = "PING";

    @Override
    protected void initialLoad() throws APIException {
        SingleValueTopicData pingTopicData = 
                TopicDataFactory.newSingleValueData(MDataType.STRING);
        pingTopicData.initialise("Welcome");
        Topic rootTopic = addTopic(ROOT_TOPIC,
                pingTopicData);
        rootTopic.setAutoSubscribe(true);
    }

    @Override
    protected void messageFromClient(TopicMessage message, Client client) {
        try {
            // As this is echo, send a message back to the same client and not
            // broadcast
            client.send(message);
        } catch (APIException ex) {
            logWarning("Unable to process message from client", ex);
        }
    }

    @Override
    protected boolean isStoppable() {
        return true;
    }
}
