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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.data.custom.CustomTopicData;
import com.pushtechnology.diffusion.api.data.custom.CustomTopicDataHandler;
import com.pushtechnology.diffusion.api.message.Record;
import com.pushtechnology.diffusion.api.message.TopicMessage;

public class MappedCustomData implements CustomTopicDataHandler {
	final HashMap<String,String> map = new HashMap<String,String>();
	final ArrayList<Map.Entry<String,String>> newEntries = new ArrayList<Map.Entry<String,String>>();
	private boolean hasChanges = false;
	
	private static class RecordEntry implements Map.Entry<String,String> {
		final private String key;
		private String value;
		public RecordEntry( String key, String value ) {
			this.key = key;
			this.value = value;
		}
		public RecordEntry( String key ) {
			this( key, null );
		}
		
		@Override public String getKey() {
			return key;
		}

		@Override public String getValue() {
			return value;
		}

		@Override public String setValue(String value) {
			this.value = value;
			return value;
		}
	}
	
	@Override public void abortUpdate() throws APIException {
		newEntries.clear();
		hasChanges = false;
	}

	@Override public String asString() {
		return "Map of " + map.size() + " entries, " + newEntries.size() + " pending changes";
	}

	@Override public boolean endUpdate() throws APIException {
		for ( Map.Entry<String,String> entry : newEntries ) {
			if ( entry.getValue() == null ) {
				map.remove( entry.getKey() );
			} else {
				map.put( entry.getKey(), entry.getValue() );
			}
		}
		return hasChanges;
	}

	@Override public boolean hasChanges() {
		return hasChanges;
	}

	@Override public void initialise(TopicMessage message) throws APIException {
		newEntries.clear();
		hasChanges = false;
		update(message);
	}

	@Override public void populateDelta(TopicMessage delta) throws APIException {
		(new MappedTopicMessage<String,String>(delta)).addEntries( newEntries );
		//Logs.info( "Populated topic delta " + delta );
	}

	@Override public void populateTopicLoad(TopicMessage topicLoad) throws APIException {
		(new MappedTopicMessage<String,String>(topicLoad)).addEntries( map.entrySet() );
		//Logs.info( "Populated topic load " + topicLoad );
	}

	@Override public void prepare() throws APIException {
		newEntries.clear();
	}

	@Override public void setTopicData(CustomTopicData topicData) {
	}

	@Override public void startUpdate() throws APIException {
		newEntries.clear();
		hasChanges = false;
	}
	
	@Override public boolean update(TopicMessage message) throws APIException {

		// Initialize if topic load
		if ( message.isTopicLoad() ) {
			hasChanges = true;
			map.clear();
		}
		
		// Record all of the entries
		for ( Record rec : message.asRecords() ) {
			if ( rec.size() == 1 ) {
				if ( map.containsKey( rec.getField(0) ) )
					newEntries.add( new RecordEntry( rec.getField(0) ) );
			} else if ( rec.size() == 2 ) {
				String currValue = map.get( rec.getField(0) );
				if ( currValue == null || !currValue.equals( rec.getField(1) ) )
					newEntries.add( new RecordEntry( rec.getField(0), rec.getField(1) ) );
			}
		}
		
		if ( !newEntries.isEmpty() )
			hasChanges = true;
		
		// Apply changes
		return hasChanges;
	}
}
