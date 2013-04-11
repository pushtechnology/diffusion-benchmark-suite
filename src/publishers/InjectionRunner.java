package publishers;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.topic.Topic;

public class InjectionRunner implements Runnable {
	public static final long PARK_NANOS_OVERHEAD = 55000;
	public static final long PARK_NANOS_THRESHOLD = 100000;

	private volatile boolean running = true;
	private final MessagePublisher messagePublisher;
	private final AtomicLong messageCounter;
	private final AtomicLong topicsCounter;
	private final InjectionConfiguration config;

	public InjectionRunner(final MessagePublisher messagePublisher,
			final AtomicLong messageCounter, final AtomicLong topicsCounter, final InjectionConfiguration config) {
		this.messagePublisher = messagePublisher;
		this.messageCounter = messageCounter;
		this.topicsCounter = topicsCounter;
		this.config = config;
	}

	public void run() {
		running = true;
		final StringBuilder messageBuilder = new StringBuilder(
            config.getMessageSize());
        for (int c = 0; c < config.getMessageSize(); c++) {
            messageBuilder.append('X');
        }
        final String data = messageBuilder.toString();
		int pauseCounter = 0;
		for (int i = 0; i < config.getInitialTopicNum(); i++) {
			messagePublisher.addChildTopic(String.valueOf(i),data);
			topicsCounter.incrementAndGet();
		}
		int topicCounter = config.getInitialTopicNum();
		
        int messagesPerIntervalPerTopic = config.getMessagesPerIntervalPerTopic();
		while (running) {
			final long timestampNanos = System.nanoTime();
			if(messagePublisher.hasClients()){
                for (int i = 0; i < messagesPerIntervalPerTopic; i++) {
					for (Topic topic : messagePublisher.getChildTopics()) {
						try {
                            messagePublisher.publish(topic, data);

                            messageCounter.lazySet(messageCounter.get() + 1);
                        }
                        catch (Exception e) {
                            Logs.severe("Runtime exception while publishing:",e);
                        }
					}
				}
			}

			boolean isCoping = applyInterval(timestampNanos);
			pauseCounter++;
            
            if (isCoping && messagePublisher.hasClients()){
                // increase number of topics
                if(pauseCounter % config.getTopicIncrementIntervalInPauses() == 0) {
                    for (int i = 0; i < config.getTopicIncrement(); i++) {
                        messagePublisher.addChildTopic(String
							.valueOf(topicCounter++),data);
                        topicsCounter.incrementAndGet();
                    }
                }
                // increase number of messages
                if(pauseCounter % config.getMessageIncrementIntervalInPauses() == 0) {
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
		if(remainingInterval < 0){
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