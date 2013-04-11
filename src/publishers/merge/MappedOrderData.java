package publishers.merge;

import java.util.List;

import com.pushtechnology.diffusion.api.message.MessageException;
import com.pushtechnology.diffusion.api.message.TopicMessage;

public class MappedOrderData {
	final public static String BID_PREFIX = "B";
	final public static String ASK_PREFIX = "A";

	public static void populateDepthDeltas( List<OrderbookChange> bidChanges, List<OrderbookChange> askChanges, TopicMessage tm ) throws MessageException
	{
		MappedTopicMessage<String,Long> mtm = new MappedTopicMessage<String,Long>( tm );
		
		// Apply bid changes
		for ( OrderbookChange change : bidChanges ) {
			if ( change.getNewQuantity() > 0 )
				mtm.put( BID_PREFIX + change.getPrice(), change.getNewQuantity() );
			else
				mtm.remove( BID_PREFIX + change.getPrice() );
		}
		
		// Apply ask changes
		for ( OrderbookChange change : askChanges ) {
			if ( change.getNewQuantity() > 0 )
				mtm.put( ASK_PREFIX + change.getPrice(), change.getNewQuantity() );
			else
				mtm.remove( ASK_PREFIX + change.getPrice() );
		}
	}
}
