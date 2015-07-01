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
package com.pushtechnology.benchmarks.clients;

import java.nio.ByteBuffer;


import com.pushtechnology.benchmarks.experiments.UnsafeMessageTimestampUtil;
import com.pushtechnology.benchmarks.monitoring.ExperimentCounters;
import com.pushtechnology.diffusion.api.message.TopicMessage;


/**
 * This client will track message latency by reading the first 8 bytes of any
 * message received and adding to it's latency histogram. The timestamp is
 * in nano seconds. This implementation bypasses the translation buffer, just
 * for fun.
 * 
 * @author nitsanw
 *
 */
public final class UnsafeLatencyMonitoringClient
    extends LatencyMonitoringClient {
    /**
     * @param experimentCountersP ...
     * @param reconnectP ...
     * @param initialTopicsP ...
     */
    public UnsafeLatencyMonitoringClient(ExperimentCounters experimentCountersP,
            boolean reconnectP, String... initialTopicsP) {
        super(experimentCountersP, reconnectP, initialTopicsP);
    }

    @Override
    protected long getArrivedTimestamp() {
        return System.nanoTime();
    }

    @Override
    protected long getSentTimestamp(TopicMessage topicMessage) {
        int headerLen = UnsafeMessageTimestampUtil.getHeaderLength(topicMessage);
        ByteBuffer buffy = UnsafeMessageTimestampUtil.getBuffer(topicMessage);
        return buffy.getLong(headerLen);
    }
}
