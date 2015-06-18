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

import org.HdrHistogram.Histogram;

import com.pushtechnology.benchmarks.experiments.CommonExperimentSettings;
import com.pushtechnology.benchmarks.monitoring.ExperimentCounters;
import com.pushtechnology.diffusion.api.message.MessageException;
import com.pushtechnology.diffusion.api.message.TopicMessage;

/**
 * This client will track message latency by reading the first 8 bytes of any
 * message received and adding to it's latency histogram. The timestamp is
 * in nano seconds.
 * 
 * @author nitsanw
 *
 */
public final class SafeLatencyMonitoringClient extends LatencyMonitoringClient {

    /** 8 bytes, aye. */
    private static final int SIZEOF_LONG = 8;
    /** we copy the first 8 bytes of the message out into this array. */
    private final byte[] timestamp = new byte[SIZEOF_LONG];
    /** we extract the long by using this byte buffer. */
    private final ByteBuffer tsWrapperBuffer = ByteBuffer.wrap(timestamp);

    public SafeLatencyMonitoringClient(ExperimentCounters experimentCountersP,
            boolean reconnectP,
			CommonExperimentSettings clientSettings, String initialTopicsP) {
        super(experimentCountersP, reconnectP, clientSettings, initialTopicsP);
    }

	@Override
    protected long getArrivedTimestamp() {
        return System.nanoTime();
    }

    @Override
    protected long getSentTimestamp(TopicMessage topicMessage) {
        try {
            topicMessage.nextBytes(timestamp);
            long sent = tsWrapperBuffer.getLong(0);
            return sent;
        } catch (MessageException e) {
            throw new RuntimeException(e);
        }
    }
}
