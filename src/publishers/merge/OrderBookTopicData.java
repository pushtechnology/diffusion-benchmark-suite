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

import java.util.List;
import java.util.NavigableMap;

import publishers.merge.OrderMatcher.OrderMatcherMonitor;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.TimeoutException;
import com.pushtechnology.diffusion.api.data.TopicDataType;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.topic.TopicClient;
import com.pushtechnology.diffusion.api.topic.TopicTreeNode;
import com.pushtechnology.diffusion.data.TopicDataImpl;

final class OrderBookTopicData extends TopicDataImpl implements OrderMatcherMonitor{
    private final String _conflationMode;
    public OrderBookTopicData(String conflationMode) {
        super();
        this._conflationMode = conflationMode;
    }
    // Create the order matcher with associated publisher
    final OrderMatcher om = new OrderMatcher(this);
    @Override
    public TopicDataType getType() {
        return TopicDataType.CUSTOM;
    }

    @Override
    public TopicMessage getLoadMessage(TopicClient client)
    throws TimeoutException, APIException {
        return getLoadMessage();
    }

    @Override
    public TopicMessage getLoadMessage() throws TimeoutException,
    APIException {
        NavigableMap<Long,Orderlist> bids = om.bidSide.orderBook;
        NavigableMap<Long,Orderlist> asks = om.askSide.orderBook;
        TopicMessage loadMessage = getTopic().createLoadMessage(20*(bids.size()+asks.size()));
        OrderBookSerializer.serializeImageMessage(bids,asks,loadMessage);
        return loadMessage;
    }

    @Override
    protected void attachedToTopic(String topicName,
        TopicTreeNode parent) throws APIException {
    }
    public void orderBookChanges(List<Trade> trades, 
                                 List<OrderbookChange> bidChanges,
                                 List<OrderbookChange> askChanges, OrderMatcher om) {
        try {
            // Create new delta
            if(!_conflationMode.equals("REPLACE")){
                TopicMessage tm = getTopic().createDeltaMessage(20*(bidChanges.size()+askChanges.size()));
                OrderBookSerializer.serializeUpdateMessage(bidChanges,askChanges,tm);
    
                // Publish it to the topic
                getTopic().publishMessage(tm);
            }
            else{
                NavigableMap<Long,Orderlist> bids = om.bidSide.orderBook;
                NavigableMap<Long,Orderlist> asks = om.askSide.orderBook;
                TopicMessage tm = getTopic().createDeltaMessage(20*(bids.size()+asks.size()));
                OrderBookSerializer.serializeImageMessage(bids,asks,tm);
            }
        }
        catch (TimeoutException e) {
            Logs.severe("Timed out publishing",e);
        }
        catch (APIException e) {
            Logs.severe("Exception publishing",e);
        }
    }

}
