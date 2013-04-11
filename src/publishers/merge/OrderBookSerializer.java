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
