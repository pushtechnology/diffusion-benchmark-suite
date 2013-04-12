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
package clients;

import com.pushtechnology.diffusion.api.Credentials;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.ServerConnectionListener;
import com.pushtechnology.diffusion.api.client.ExternalClientConnection;
import com.pushtechnology.diffusion.api.message.MessageException;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.topic.TopicStatus;

import java.util.concurrent.atomic.AtomicLong;

import publishers.InjectionPublisher;
import util.IntervalCounterMonitor;


public final class ConsumptionThroughputTestClient
    implements ServerConnectionListener
{
    public static final String CONNECT_STRING;

    static
    {
        CONNECT_STRING = System.getProperty("connect.string", "ws://localhost:8080");
    }

    private final AtomicLong messageCounter = new AtomicLong();
    private final IntervalCounterMonitor intervalCounterMonitor = new IntervalCounterMonitor(messageCounter, "messages received");
    private String latestMessage;

    public static void main(final String[] args)
        throws Exception
    {
        ConsumptionThroughputTestClient consumptionThroughputTestClient = new ConsumptionThroughputTestClient();

        Thread t = new Thread(consumptionThroughputTestClient.intervalCounterMonitor);
        t.setDaemon(true);
        t.setName("counter-monitor-thread");
        t.start();

        ExternalClientConnection connection = new ExternalClientConnection(consumptionThroughputTestClient, CONNECT_STRING);
        connection.connect();
        connection.subscribe(InjectionPublisher.INJECTOR_ROOT);

        System.out.println("Hit any key to exit");
        System.in.read();

        consumptionThroughputTestClient.intervalCounterMonitor.halt();
        t.interrupt();

        System.out.println("exiting: latest message = " + consumptionThroughputTestClient.latestMessage);
    }

    @Override
    public void serverConnected(final ServerConnection serverConnection)
    {
        System.out.println("ConsumptionThroughputTestClient.serverConnected");
    }

    @Override
    public void messageFromServer(final ServerConnection serverConnection, final TopicMessage topicMessage)
    {
        try
        {
            latestMessage = topicMessage.asString();
            messageCounter.lazySet(messageCounter.get() + 1);
        }
        catch (MessageException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void serverTopicStatusChanged(final ServerConnection serverConnection, final String s, final TopicStatus topicStatus)
    {
        System.out.println("ConsumptionThroughputTestClient.serverTopicStatusChanged");
    }

    @Override
    public void serverRejectedCredentials(final ServerConnection serverConnection, final Credentials credentials)
    {
        System.out.println("ConsumptionThroughputTestClient.serverRejectedCredentials");
    }

    @Override
    public void serverDisconnected(final ServerConnection serverConnection)
    {
        System.out.println("ConsumptionThroughputTestClient.serverDisconnected");
    }
}
