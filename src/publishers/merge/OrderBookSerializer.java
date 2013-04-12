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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.pushtechnology.diffusion.api.message.DataMessage;
import com.pushtechnology.diffusion.api.message.MessageException;
import com.pushtechnology.diffusion.api.message.Record;

public final class OrderBookSerializer {
    private OrderBookSerializer() {
    }

    public static void serializeUpdateMessage(List<OrderbookChange> bidChanges,
    List<OrderbookChange> askChanges,DataMessage update)
    throws MessageException {
        for (OrderbookChange change:bidChanges) {
            if (change.getNewQuantity()!=0)
                update.putRecord("B",String.valueOf(change.getPrice()),
                    String.valueOf(change.getNewQuantity()));
            else
                update.putRecord("B",String.valueOf(change.getPrice()));
        }
        for (OrderbookChange change:askChanges) {
            if (change.getNewQuantity()!=0)
                update.putRecord("A",String.valueOf(change.getPrice()),
                    String.valueOf(change.getNewQuantity()));
            else
                update.putRecord("A",String.valueOf(change.getPrice()));
        }
    }

    public static void serializeImageMessage(Map<Long,Orderlist> bids,
    Map<Long,Orderlist> asks,DataMessage update)
    throws MessageException {
        for (Entry<Long,Orderlist> e:bids.entrySet()) {
            update.putRecord("B",String.valueOf(e.getKey()),
                String.valueOf(e.getValue().getQuantity()));
        }
        for (Entry<Long,Orderlist> e:asks.entrySet()) {
            update.putRecord("A",String.valueOf(e.getKey()),
                String.valueOf(e.getValue().getQuantity()));
        }
    }

    public static void serializeUpdateMessage(
    LinkedHashMap<String,String> bids,
    LinkedHashMap<String,String> asks,DataMessage update)
    throws MessageException {
        for (Entry<String,String> bid:bids.entrySet()) {
            if (bid.getValue()!=MarketDepthPublisher.ZERO) {
                update.putRecord("B",bid.getKey(),bid.getValue());
            }
            else {
                update.putRecord("B",bid.getKey());
            }
        }
        for (Entry<String,String> ask:asks.entrySet()) {
            if (ask.getValue()!=MarketDepthPublisher.ZERO) {
                update.putRecord("A",ask.getKey(),ask.getValue());
            }
            else {
                update.putRecord("A",ask.getKey());
            }
        }
    }

    public static void deserializeIntoMap(DataMessage message,
    Map<String,String> bids,Map<String,String> asks)
    throws MessageException {
        List<Record> curr = message.asRecords();
        for (Record r:curr) {
            if (r.getField(0).equals("B")) {
                if (r.size()==3)
                    bids.put(r.getField(1),r.getField(2));
                else
                    bids.put(r.getField(1),MarketDepthPublisher.ZERO);
            }
            else {
                if (r.size()==3)
                    asks.put(r.getField(1),r.getField(2));
                else
                    asks.put(r.getField(1),MarketDepthPublisher.ZERO);
            }
        }
    }
}
