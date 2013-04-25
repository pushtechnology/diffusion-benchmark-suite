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
package publishers;

import java.util.concurrent.atomic.AtomicLong;

import util.IntervalCounterMonitor;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.data.TopicDataFactory;
import com.pushtechnology.diffusion.api.data.custom.SimpleCustomTopicDataHandler;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.publisher.Client;
import com.pushtechnology.diffusion.api.publisher.EventConnection;
import com.pushtechnology.diffusion.api.publisher.Publisher;
import com.pushtechnology.diffusion.api.topic.Topic;

public final class ChatterPublisher extends Publisher {

    public static final String TOPIC = "CHATTER";
    private Topic rootTopic;
    AtomicLong pingCounter = new AtomicLong(0L);
    private IntervalCounterMonitor intervalCounterMonitor;
    private Thread monitorThread;

    @Override
    protected void initialLoad() throws APIException {
        System.out.println("InjectionPublisher.initialLoad");
        rootTopic = addTopic(TOPIC,
                TopicDataFactory
                        .newCustomData(new SimpleCustomTopicDataHandler() {

                            @Override
                            public void populateTopicLoad(TopicMessage topicLoad)
                                    throws APIException {
                            }

                            @Override
                            public void populateDelta(TopicMessage delta)
                                    throws APIException {
                            }

                            @Override
                            public String asString() {
                                return "";
                            }

                        }));
        rootTopic.setAutoSubscribe(true);
        intervalCounterMonitor = new IntervalCounterMonitor(pingCounter,
                "pings");
        monitorThread = new Thread(intervalCounterMonitor);
        monitorThread.setDaemon(true);
        monitorThread.setName("counter-monitor-thread");
        monitorThread.start();
    }

    @Override
    protected void publisherStopped() throws APIException {
        if (monitorThread == null) {
            return;
        }
        intervalCounterMonitor.halt();
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        monitorThread = null;
    }

    @Override
    protected void messageFromClient(TopicMessage message, Client client) {
        try {
            // we broadcast to all clients except the one who sent the message
            rootTopic.publishExclusiveMessage(message, client);
            pingCounter.incrementAndGet();
        } catch (APIException ex) {
            logWarning("Unable to process message from client", ex);
        }
    }
    
    @Override
    protected boolean isStoppable() {
        return true;
    }
}
