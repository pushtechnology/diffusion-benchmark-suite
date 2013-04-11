package publishers;

public class InjectionConfiguration {
    private final int messageSize;
    private final long intervalPauseNanos;

    private final int initialMessages;
    private final long messageIncrementIntervalInPauses;
    private final int messageIncrement;

    private final int initialTopics;
    private final long topicIncrementIntervalInPauses;
    private final int topicIncrementPerInterval;


    public InjectionConfiguration(int messageSize,long intervalPauseNanos,
                                  int initialMessages,
                                  long messageIncrementInterval,
                                  int messageIncrementPerInterval,
                                  int initialTopicNum,
                                  long topicIncrementInterval,
                                  int topicIncrementPerInterval) {
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
