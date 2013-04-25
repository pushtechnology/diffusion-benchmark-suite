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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Orderlist {

    final private TreeMap<Long, Order> orderList = new TreeMap<Long, Order>();
    private long qty = 0;

    public long getQuantity() {
        return qty;
    }

    public int getOrderCount() {
        return orderList.size();
    }

    public long matchOrder(Order addOrder, List<Trade> trades) {
        // if ( log.isDebugEnabled() )
        // log.debug( "Submitting order isBid:" + addOrder.getIsBid() +
        // " price:" + addOrder.getPrice() + " isGTC:" + addOrder.getIsGTC() +
        // " order:" + addOrder.getRemainingQty() + " orderId:" +
        // addOrder.getOrderId() + " REF:" + addOrder );

        if (addOrder.getRemainingQty() == 0)
            return qty;

        long cQty = qty;

        // Notify listeners of start of update
        // for ( MatcherChangeListener listener : listeners )
        // listener.startUpdate();

        // Match it
        Iterator<Map.Entry<Long, Order>> listIter = orderList.entrySet()
                .iterator();

        long totalMatched = 0;
        while (listIter.hasNext() && addOrder.getRemainingQty() > 0) {
            Map.Entry<Long, Order> listEntry = listIter.next();

            // Match it
            long matchedQty = Math.min(listEntry.getValue().getRemainingQty(),
                    addOrder.getRemainingQty());
            addOrder.match(listEntry.getValue(), listEntry.getValue()
                    .getPrice(), true, matchedQty);
            listEntry.getValue().match(addOrder,
                    listEntry.getValue().getPrice(), false, matchedQty);
            totalMatched += matchedQty;

            // Add to trades
            trades.add(new Trade(listEntry.getValue().getPrice(), matchedQty));

            // If done remove it from list ..
            if (listEntry.getValue().getRemainingQty() == 0)
                listIter.remove();
        }

        qty -= totalMatched;

        // Done
        return qty - cQty;
    }

    public long addOrder(Order addOrder) {
        long cQty = qty;
        qty += addOrder.getRemainingQty();
        Order oldOrder = orderList.put(addOrder.getOrderId(), addOrder);
        if (oldOrder != null)
            qty -= oldOrder.getRemainingQty();
        return qty - cQty;
    }

    public long removeOrder(Order addOrder) {
        long cQty = qty;
        Order oldOrder = orderList.remove(addOrder.getOrderId());
        qty -= oldOrder.getRemainingQty();
        return qty - cQty;
    }
}
