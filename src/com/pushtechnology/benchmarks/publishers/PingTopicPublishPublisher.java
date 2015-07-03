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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public final class PingTopicPublishPublisher extends Publisher {
    /** the ping topic. Messages sent here will be sent back. */
    public static final String ROOT_TOPIC = "PING_TOPIC_PUBLISH";
    private static final Logger LOG = LoggerFactory.getLogger(PingTopicPublishPublisher.class);    /** the ping topic, messages will be re-broadcasted on this one.*/
    private Topic rootTopic;
    private Topic[] topics;

    @Override
    protected void initialLoad() throws APIException {
        SingleValueTopicData pingTopicData = 
                TopicDataFactory.newSingleValueData(MDataType.STRING);
        
        String configClients = this.getConfig().getPropertyValue("NumberOfClients");
        int numClients=0;
        if(configClients != null){
        	try{
        		numClients = Integer.parseInt(configClients);
        		topics = new Topic[numClients];
        	} catch(NumberFormatException ignore){}
        }
        
        pingTopicData.initialise("Welcome");
        
        if(topics == null){
        rootTopic = addTopic(ROOT_TOPIC,
                pingTopicData);
        rootTopic.setAutoSubscribe(true);
        } else {
        	for(int i=0;i<numClients;i++){
                topics[i] = addTopic(ROOT_TOPIC+"_"+i,
                        pingTopicData);
                topics[i].setAutoSubscribe(true);
        	}
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void messageFromClient(TopicMessage message, Client client) {
        try {
        	//TODO : determine which topic to respond on
        	
            // Echo to the topic
            rootTopic.publishMessage(message);
        } catch (APIException ex) {
            LOG.warn("Unable to process message from client", ex);
        }
    }

    @Override
    protected boolean isStoppable() {
        return true;
    }
}
