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

import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import publishers.merge.Order.MatchableOrderListener;
import publishers.merge.OrderMatcher.OrderMatcherMonitor;

public class OrderSimulator {

    final static private Random rand = new Random();

    final private static Timer timer = new Timer("OrderSimulator", true);

    final static long CANCEL_DELAY = 10000;
    final static long MIN_FREQUENCY = 10;
    final static long MAX_FREQUENCY = 1000;

    private static final int CANCEL_QUEUE = 10;

    static public void main(String[] args) {
        // System.setProperty( "org.slf4j.simpleLogger.logFile", "System.out" );
        // System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "debug"
        // );
        // System.setProperty( SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG" );
        // log.info( "HELLO" );
        // log.debug( "HELLO" );
        System.out.println("Hi.");
        OrderMatcher om = new OrderMatcher(new OrderMatcherMonitor() {
            final TreeMap<Long, Long> bids = new TreeMap<Long, Long>();
            final TreeMap<Long, Long> asks = new TreeMap<Long, Long>();

            @Override
            public void orderBookChanges(List<Trade> trades,
                    List<OrderbookChange> bidChanges,
                    List<OrderbookChange> askChanges, OrderMatcher om) {

                StringBuffer sb = new StringBuffer();
                sb.append("Trades: [");
                for (Trade trade : trades)
                    sb.append(trade.getPrice() + "=" + trade.getQuantity()
                            + ";");
                sb.append("], Bids: [");
                for (OrderbookChange change : bidChanges)
                    sb.append(change.getPrice() + "=" + change.getNewQuantity()
                            + ";");
                sb.append("], Asks: [");
                for (OrderbookChange change : askChanges)
                    sb.append(change.getPrice() + "=" + change.getNewQuantity()
                            + ";");
                sb.append("]");
                // log.info( "Orderbook update: " + sb.toString() );

                applyChanges("bid", bids, bidChanges);
                applyChanges("ask", asks, askChanges);
                // log.debug( "Best bid: " +
                // (bids.isEmpty()?"NA":bids.lastEntry().getKey()) +
                // " Best ask: " +
                // (asks.isEmpty()?"NA":asks.firstEntry().getKey()) );
            }

            private void applyChanges(String side, TreeMap<Long, Long> map,
                    List<OrderbookChange> changes) {
                for (OrderbookChange change : changes) {
                    // if ( log.isDebugEnabled() )
                    // log.debug( "Book change: " + side + " " +
                    // change.getPrice() + ": " + change.getNewQuantity() );
                    if (change.getNewQuantity() > 0)
                        map.put(change.getPrice(), change.getNewQuantity());
                    else
                        map.remove(change.getPrice());
                }
                // if ( log.isDebugEnabled() )
                // log.debug( "Book change is done." );
            }
        });
        OrderSimulator os = new OrderSimulator(om, 100);
        os.setOrdersPerSecond(100);
        System.out.println("Done?");
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    final private OrderMatcher om;

    private long minQty = 10;

    public void setMinQty(long minQty) {
        this.minQty = minQty;
    }

    private long maxQty = 100;

    public void setMaxQty(long maxQty) {
        this.maxQty = maxQty;
    }

    private long vwapTimeout = 5000;

    public long getVWAPTimeout() {
        return vwapTimeout;
    }

    public void setVWAPTimeout(long vwapTimeout) {
        this.vwapTimeout = vwapTimeout;
    }

    private double simulatorOrdersPerFreq = 1.0;
    private long simulatorFrequency = 1000;

    public double getOrdersPerSecond() {
        return simulatorOrdersPerFreq * (1000 / simulatorFrequency);
    }

    public void setOrdersPerSecond(double simulatorOrdersPerSecond) {
        if (simulatorOrdersPerSecond <= 0.0) {
            simulatorFrequency = MAX_FREQUENCY;
            simulatorOrdersPerFreq = 0;
            return;
        }

        double freq = (1.0 / simulatorOrdersPerSecond) * 1000.0;
        if (freq < MIN_FREQUENCY)
            simulatorFrequency = MIN_FREQUENCY;
        else if (freq > MAX_FREQUENCY)
            simulatorFrequency = MAX_FREQUENCY;
        else
            simulatorFrequency = (long) freq;

        simulatorOrdersPerFreq = (simulatorOrdersPerSecond * (simulatorFrequency / 1000.0));
    }

    private double lastSimulatedOrdersError = 0;

    // private
    public OrderSimulator(OrderMatcher om, long startPrice) {
        this.om = om;
        lastVWAP = startPrice;
        scheduleOrderSubmit();
    }

    private volatile long lastVWAP;
    private volatile long vwapQty;
    private volatile long vwapPriceQty;

    private void addToVWAP(final long price, final long qty) {
        final long priceQty = price * qty;
        vwapQty += qty;
        vwapPriceQty += priceQty;
        lastVWAP = Math.round(vwapPriceQty / vwapQty);

        // if ( log.isDebugEnabled() )
        // log.debug( "VWAP has [" + price + "," + qty +
        // "] as new trade, latest VWAP " + lastVWAP );

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                vwapQty -= qty;
                vwapPriceQty -= priceQty;
                if (vwapQty > 0) {
                    lastVWAP = Math.round(vwapPriceQty / vwapQty);
                    // if ( log.isDebugEnabled() )
                    // log.debug( "VWAP has [" + price + "," + qty +
                    // "] as old trade, latest VWAP " + lastVWAP );
                }
            }
        }, vwapTimeout);
    }

    private long getVWAP() {
        return lastVWAP;
    }

    private void scheduleOrderSubmit() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                // Calculate desired orders to send as well as actual
                double ordersToSend = simulatorOrdersPerFreq
                        - lastSimulatedOrdersError;
                int cnt = (int) ordersToSend;

                // Send them!
                for (int i = 0; i < cnt; i++)
                    sendSimulatedOrder();

                // New error
                lastSimulatedOrdersError = cnt - ordersToSend;

                // Schedule next one appropriately
                scheduleOrderSubmit();
            }
        }, simulatorFrequency);
    }

    private long buySellBias = 50;
    final private ArrayDeque<TimerTask> ordersToBeCancelled = new ArrayDeque<TimerTask>(
            CANCEL_QUEUE);

    private void sendSimulatedOrder() {

        // Adjust buy/sell bias infrequently
        if (rand.nextInt(100) < 3) {
            buySellBias = rand.nextInt(100);
            if (buySellBias >= 70)
                buySellBias = 65;
            else if (buySellBias < 30)
                buySellBias = 35;
        }

        // Determine side
        final boolean side = (rand.nextInt(100) < buySellBias);

        // Calculate quantity
        final long qty = (Math.abs(rand.nextLong()) % (maxQty - minQty))
                + minQty;

        final int SHIFT = 3;
        final int RANGE = 10;
        // Calculate price
        final long price = getVWAP() + (rand.nextInt(RANGE + 1) - (RANGE / 2))
                + ((side) ? -SHIFT : SHIFT);

        // log.debug( "Price delta = " + (price - getVWAP()) );
        // System.out.println( "Order of qty " + qty + " from max " + maxQty +
        // " min " + minQty );

        final Order o = new Order(price, qty, side, true);

        final TimerTask cancelTask = new TimerTask() {
            @Override
            public void run() {
                om.cancelOrder(o);
            }
        };

        // Sitting order..
        timer.schedule(cancelTask, CANCEL_DELAY);

        o.setMatchableOrderListener(new MatchableOrderListener() {
            @Override
            public void trade(Order order, Order matchOrder, boolean isAggress,
                    long price, long quantity) {
                addToVWAP(price, quantity);
            }

            @Override
            public void finish(Order order) {
                cancelTask.cancel();
            }
        });

        om.submitOrder(o);

        // Cancel anything remaining
        while (ordersToBeCancelled.size() >= CANCEL_QUEUE) {
            TimerTask qcancelTask = ordersToBeCancelled.pop();
            if (qcancelTask.cancel())
                qcancelTask.run();
        }

        // Add to end of list as well..
        ordersToBeCancelled.addLast(cancelTask);
    }
}
