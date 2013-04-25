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

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class Orderbook {

    final static private Comparator<Long> descendingComparator = new Comparator<Long>() {
        @Override
        public int compare(Long arg0, Long arg1) {
            return (int) (arg1 - arg0);
        }
    };

    final NavigableMap<Long, Orderlist> orderBook;

    public Orderbook(boolean isBid) {
        if (isBid) {
            orderBook = new TreeMap<Long, Orderlist>(descendingComparator);
        } else {
            orderBook = new TreeMap<Long, Orderlist>();
        }
    }

    public void addOrder(Order addOrder, List<OrderbookChange> orderbook) {
        Orderlist orderlist = orderBook.get(addOrder.getPrice());
        if (orderlist == null) {
            orderlist = new Orderlist();
            orderBook.put(addOrder.getPrice(), orderlist);
        }
        orderlist.addOrder(addOrder);
        orderbook.add(new OrderbookChange(addOrder.getPrice(), orderlist
                .getQuantity()));
    }

    public void removeOrder(Order addOrder, List<OrderbookChange> orderbook) {
        Orderlist orderlist = orderBook.get(addOrder.getPrice());

        if (orderlist == null)
            return;

        orderlist.removeOrder(addOrder);

        orderbook.add(new OrderbookChange(addOrder.getPrice(), orderlist
                .getQuantity()));

        if (orderlist.getQuantity() == 0) {
            orderBook.remove(addOrder.getPrice());
        }
    }

    /**
     * Submit an order to be matched
     * 
     * @param isBid
     *            True if bid, false if offer
     * @param price
     *            Price of order
     * @param isGTC
     *            True if order is good until cancelled
     * @param addOrder
     *            Order to process
     * @return
     */
    public void matchOrder(Order addOrder, List<OrderbookChange> orderbook,
            List<Trade> trades) {

        // if ( log.isDebugEnabled() )
        // log.debug( "Submitting order: " + addOrder );

        if (addOrder.getRemainingQty() == 0)
            return;

        Iterator<Map.Entry<Long, Orderlist>> iter = orderBook.entrySet()
                .iterator();
        while (iter.hasNext() && addOrder.getRemainingQty() > 0) {
            Map.Entry<Long, Orderlist> entry = iter.next();

            // Break if price exceeded
            if (addOrder.getIsBid()) {
                if (addOrder.getPrice() < entry.getKey())
                    break;
            } else {
                if (addOrder.getPrice() > entry.getKey())
                    break;
            }

            if (entry.getValue().matchOrder(addOrder, trades) != 0)
                orderbook.add(new OrderbookChange(entry.getKey(), entry
                        .getValue().getQuantity()));

            if (entry.getValue().getQuantity() == 0) {
                iter.remove();
            }
        }

        // Done
        return;
    }

    @Override
    public String toString() {
        // StringBuffer sb = new StringBuffer();
        // for ( Map.Entry<Long,Orderbook>)
        return "";
    }
}
