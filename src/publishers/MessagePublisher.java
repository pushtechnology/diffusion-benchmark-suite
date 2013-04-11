package publishers;

import java.util.Queue;

import com.pushtechnology.diffusion.api.topic.Topic;

public interface MessagePublisher
{
    void addChildTopic(String topic, String loaddata);
	void publish(Topic topic, String... message);
	Queue<Topic> getChildTopics();
	boolean hasClients();
}
