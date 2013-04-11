package clients;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.message.TopicMessage;

final class TopicCountingConnectionCallback extends
MessageCountingConnectionCallback {
    private final Set<String> topics = new HashSet<String>();
    private final AtomicLong topicsCounter;

    TopicCountingConnectionCallback(AtomicLong messageCounter,
                                    AtomicLong bytesCounter,
                                    AtomicLong clientConnectCounter,
                                    AtomicLong clientDisconnectCounter,
                                    AtomicLong connectionRefusedCounter,
                                    AtomicLong topicsCounter) {
        super(messageCounter,bytesCounter,clientConnectCounter,
            clientDisconnectCounter,connectionRefusedCounter);
        this.topicsCounter = topicsCounter;
    }

    @Override
    public void messageFromServer(ServerConnection serverConnection,
    TopicMessage topicMessage) {
        super.messageFromServer(serverConnection,topicMessage);
        int size = topics.size();
        topics.add(topicMessage.getTopicName());
        if (size!=topics.size()) {
            topicsCounter.set(topics.size());
        }
    }
}