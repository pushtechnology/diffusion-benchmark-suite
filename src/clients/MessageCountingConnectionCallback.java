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

import java.util.concurrent.atomic.AtomicLong;

import com.pushtechnology.diffusion.api.Credentials;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.ServerConnectionListener;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.topic.TopicStatus;
import com.pushtechnology.diffusion.message.TopicMessageImpl;

public class MessageCountingConnectionCallback implements
ServerConnectionListener {
    private final AtomicLong messageCounter;
    private final AtomicLong clientConnectCounter;
    private final AtomicLong clientDisconnectCounter;
    private final AtomicLong connectionRefusedCounter;
    private final AtomicLong bytesCounter;
    private final boolean reconnect;

    public MessageCountingConnectionCallback(AtomicLong messageCounter,
    AtomicLong bytesCounter,
    AtomicLong clientConnectCounter,
    AtomicLong clientDisconnectCounter,
    AtomicLong connectionRefusedCounter) {
        this(messageCounter,bytesCounter,
            clientConnectCounter,
            clientDisconnectCounter,
            connectionRefusedCounter,true);
    }

    public MessageCountingConnectionCallback(AtomicLong messageCounter,
    AtomicLong bytesCounter,
    AtomicLong clientConnectCounter,
    AtomicLong clientDisconnectCounter,
    AtomicLong connectionRefusedCounter,
    boolean reconnect) {
        super();
        this.messageCounter = messageCounter;
        this.clientConnectCounter = clientConnectCounter;
        this.clientDisconnectCounter = clientDisconnectCounter;
        this.connectionRefusedCounter = connectionRefusedCounter;
        this.bytesCounter = bytesCounter;
        this.reconnect = reconnect;
    }

    @Override
    public void serverConnected(final ServerConnection serverConnection) {
        clientConnectCounter.incrementAndGet();
    }

    @Override
    public void messageFromServer(final ServerConnection serverConnection,
    final TopicMessage topicMessage) {
        messageCounter.incrementAndGet();
        bytesCounter.addAndGet(((TopicMessageImpl)topicMessage).getOriginalMessageSize());
    }

    @Override
    public void serverTopicStatusChanged(
    final ServerConnection serverConnection,final String s,
    final TopicStatus topicStatus) {
    }

    @Override
    public void serverRejectedCredentials(
    final ServerConnection serverConnection,
    final Credentials credentials) {
    }

    @Override
    public void serverDisconnected(final ServerConnection serverConnection) {
        clientDisconnectCounter.incrementAndGet();
        if (reconnect) {
            try {
                serverConnection.connect();
            }
            catch (Exception e) {
                connectionRefusedCounter.incrementAndGet();
            }
        }
    }
}
