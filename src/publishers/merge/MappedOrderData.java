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

import java.util.List;

import com.pushtechnology.diffusion.api.message.MessageException;
import com.pushtechnology.diffusion.api.message.TopicMessage;

public class MappedOrderData {
    final public static String BID_PREFIX = "B";
    final public static String ASK_PREFIX = "A";

    public static void populateDepthDeltas(List<OrderbookChange> bidChanges,
            List<OrderbookChange> askChanges, TopicMessage tm)
            throws MessageException {
        MappedTopicMessage<String, Long> mtm = new MappedTopicMessage<String, Long>(
                tm);

        // Apply bid changes
        for (OrderbookChange change : bidChanges) {
            if (change.getNewQuantity() > 0)
                mtm.put(BID_PREFIX + change.getPrice(), change.getNewQuantity());
            else
                mtm.remove(BID_PREFIX + change.getPrice());
        }

        // Apply ask changes
        for (OrderbookChange change : askChanges) {
            if (change.getNewQuantity() > 0)
                mtm.put(ASK_PREFIX + change.getPrice(), change.getNewQuantity());
            else
                mtm.remove(ASK_PREFIX + change.getPrice());
        }
    }
}
