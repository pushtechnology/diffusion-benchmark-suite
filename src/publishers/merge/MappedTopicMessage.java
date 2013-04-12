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
