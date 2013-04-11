package publishers.merge;

import java.util.concurrent.atomic.AtomicLong;

public class Order
{
	static final private AtomicLong orderIdGenerator = new AtomicLong(0);
	
	public static interface MatchableOrderListener {
		public void trade( Order order, Order matchOrder, boolean isAggress, long price, long quantity );
		public void finish( Order order );
	}

	final private long orderId, price, quantity;
	final private boolean isBid, isGTC;
	public Order( long price, long quantity, boolean isBid, boolean isGTC ) {
		this.orderId = orderIdGenerator.incrementAndGet();
		this.price = price;
		this.quantity = quantity;
		this.isBid = isBid;
		this.isGTC = isGTC;
		this.qtyRemaining = this.quantity;
	}
	
	final public long getPrice() {
		return price;
	}
	
	final public long getOrderQuantity() {
		return quantity;
	}
	
	final public long getOrderId() {
		return orderId;
	}
	
	final public boolean getIsBid() {
		return isBid;
	}
	
	final public boolean getIsGTC() {
		return isGTC;
	}
	
	private long qtyRemaining = 0;
	final public long getRemainingQty() {
		return qtyRemaining;
	}
	
	// Protected for this package!
	final void match( Order order, long price, boolean isAggressor, long quantity ) {
		qtyRemaining -= quantity;
		if ( listener != null ) {
			listener.trade( this, order, isAggressor, price, quantity );
			listener.finish( this );
		}
	}
	
	private MatchableOrderListener listener;
	final public void setMatchableOrderListener( MatchableOrderListener listener ) {
		this.listener = listener;
		if ( listener != null && qtyRemaining <= 0 ) {
			listener.finish( this );
		}
	}
	
	@Override public String toString() {
		return "Order[id:" + orderId + " price:" +price+" quantity:" + qtyRemaining +"/" +quantity +" isBid:" + isBid + " isGTC:" + isGTC + "]";
	}
}