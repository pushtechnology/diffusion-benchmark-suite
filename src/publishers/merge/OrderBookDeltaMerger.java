package publishers.merge;

import java.util.LinkedHashMap;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.conflation.MessageMerger;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.message.DeltaMessage;

final class OrderBookDeltaMerger implements MessageMerger {
    @Override
    public TopicMessage merge(TopicMessage currentMessage,
    TopicMessage newMessage) throws APIException {
        if (currentMessage.isDelta()&&newMessage.isDelta()) {
            LinkedHashMap<String,String> bids = new LinkedHashMap<String,String>();
            LinkedHashMap<String,String> asks = new LinkedHashMap<String,String>();
            
            OrderBookSerializer.deserializeIntoMap(currentMessage,bids,asks);
            OrderBookSerializer.deserializeIntoMap(newMessage,bids,asks);
            DeltaMessage merged =
                new DeltaMessage(currentMessage.getTopicName(), 
                    20*(bids.size()+asks.size()));
            OrderBookSerializer.serializeUpdateMessage(bids,asks,merged);
            return merged;
        }
        return currentMessage;
    }

    

    
}