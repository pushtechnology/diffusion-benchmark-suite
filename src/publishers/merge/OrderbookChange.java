package publishers.merge;

public class OrderbookChange {
	public OrderbookChange( long price, long newQuantity ) {
		this.price = price;
		this.newQuantity = newQuantity;
	}
	private long price;
	public long getPrice() { return price; }
	private long newQuantity;
	public long getNewQuantity() { return newQuantity; }
}