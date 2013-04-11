package publishers.merge;

public class Trade {
	public Trade( long price, long quantity ) {
		this.price = price;
		this.quantity = quantity;
	}
	final private long price;
	final private long quantity;
	public long getPrice() {
		return price;
	}
	public long getQuantity() {
		return quantity;
	}
}