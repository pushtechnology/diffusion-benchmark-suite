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
