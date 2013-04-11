package publishers.merge;

import java.util.ArrayList;
import java.util.List;

public class OrderMatcher {
	
	public interface OrderMatcherMonitor {
		public void orderBookChanges( List<Trade> trades, List< OrderbookChange > bidChanges, List< OrderbookChange > askChanges, OrderMatcher om );
	}

	final Orderbook bidSide = new Orderbook(true);
	final Orderbook askSide = new Orderbook(false);
	final private OrderMatcherMonitor monitor;

	public OrderMatcher( OrderMatcherMonitor monitor ) {
		this.monitor = monitor;
	}
	
	/**
	 * Submit an order to be matched
	 * 
	 * @param isBid    True if bid, false if offer
	 * @param price    Price of order
	 * @param isGTC    True if order is good until cancelled
	 * @param addOrder Order to process
	 * @return
	 */
	public void submitOrder( Order addOrder ) {
		ArrayList< OrderbookChange > bidChanges = new ArrayList< OrderbookChange >();
		ArrayList< OrderbookChange > askChanges = new ArrayList< OrderbookChange >();
		ArrayList<Trade> trades = new ArrayList< Trade >();

		if ( addOrder.getRemainingQty() == 0 )
			return;

		if ( addOrder.getIsBid() ) {
			askSide.matchOrder( addOrder, askChanges, trades );
		} else {
			bidSide.matchOrder( addOrder, bidChanges, trades );
		}

		if ( addOrder.getRemainingQty() > 0 && addOrder.getIsGTC() )
		{
			if ( addOrder.getIsBid() ) {
				bidSide.addOrder( addOrder, bidChanges );
			} else {
				askSide.addOrder( addOrder, askChanges );
			}
		}
		
		monitor.orderBookChanges( trades, bidChanges, askChanges, this );
	}
	
	/**
	 * Cancel the order at the specified price and of the specified orderId
	 * @param isBid   True if bid, false if offer
	 * @param price   Price of order placed
	 * @param orderId OrderId (after submitting)
	 * @return True if order successfully removed
	 */
	public void cancelOrder( Order order ) {
		ArrayList< OrderbookChange > bidChanges = new ArrayList< OrderbookChange >();
		ArrayList< OrderbookChange > askChanges = new ArrayList< OrderbookChange >();
		ArrayList<Trade> trades = new ArrayList< Trade >();

		if ( order.getIsBid() ) {
			bidSide.removeOrder( order, bidChanges );
		}
		else {
			askSide.removeOrder( order, askChanges );
		}
		
		monitor.orderBookChanges( trades, bidChanges, askChanges, this );
	}
	
	@Override public String toString() {
		return "Bids:" + bidSide + " Asks:" + askSide;
	}
}