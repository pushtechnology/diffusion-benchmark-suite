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

import java.util.LinkedHashMap;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.conflation.MessageMerger;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.message.DeltaMessage;

final class OrderBookDeltaMerger implements MessageMerger {
    @Override
    public TopicMessage merge(TopicMessage currentMessage,
            TopicMessage newMessage) throws APIException {
        if (currentMessage.isDelta() && newMessage.isDelta()) {
            LinkedHashMap<String, String> bids = new LinkedHashMap<String, String>();
            LinkedHashMap<String, String> asks = new LinkedHashMap<String, String>();

            OrderBookSerializer.deserializeIntoMap(currentMessage, bids, asks);
            OrderBookSerializer.deserializeIntoMap(newMessage, bids, asks);
            DeltaMessage merged = new DeltaMessage(
                    currentMessage.getTopicName(),
                    20 * (bids.size() + asks.size()));
            OrderBookSerializer.serializeUpdateMessage(bids, asks, merged);
            return merged;
        }
        return currentMessage;
    }

}
