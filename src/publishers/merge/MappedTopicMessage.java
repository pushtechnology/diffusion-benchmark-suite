package publishers.merge;

import java.util.Map;

import com.pushtechnology.diffusion.api.message.MessageException;
import com.pushtechnology.diffusion.api.message.TopicMessage;

public class MappedTopicMessage<K,V> {
	final private TopicMessage topicMessage;
	
	public MappedTopicMessage( TopicMessage topicMessage ) {
		this.topicMessage = topicMessage;
	}
	
	public void put( K key, V value ) throws MessageException {
		topicMessage.putRecord( key.toString(), value.toString() );
	}
	
	public void remove( K key ) throws MessageException {
		topicMessage.putRecord( key.toString() );
	}
	
	public void addEntry( Map.Entry<K,V> entry ) throws MessageException {
		if ( entry.getValue() == null )
			remove( entry.getKey() );
		else
			put( entry.getKey(), entry.getValue() );
	}
	
	public void addEntries( Iterable<Map.Entry<K,V>> entries ) throws MessageException {
		for ( Map.Entry<K,V> entry : entries )
			addEntry( entry );
	}
}
