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
package com.pushtechnology.benchmarks.experiments;

import com.pushtechnology.benchmarks.monitoring.ExperimentCounters;

/**
 * Straight load increase over time with set pauses.
 * 
 * @author nitsanw
 * 
 */
public class DefaultLoadStrategy implements ExperimentLoadStrategy {
    /** millis per second. */
    private static final int MILLIS_PER_SECOND = 1000;
    /** configuration. */
    private final CommonExperimentSettings clientSettings;
    /** counters. */
    private final ExperimentCounters experimentCounters;

    /**
     * @param clientSettingsP ...
     * @param experimentCountersP ...
     */
    public DefaultLoadStrategy(CommonExperimentSettings clientSettingsP,
            ExperimentCounters experimentCountersP) {
        this.clientSettings = clientSettingsP;
        this.experimentCounters = experimentCountersP;
    }

    // CHECKSTYLE:OFF
    @Override
    public boolean testNotOver(long testStartTime) {
        // CHECKSTYLE:ON
        boolean timeNotOver = clientSettings.getMaxTestTimeMillis() == 0
                || System.currentTimeMillis() - testStartTime
                    < clientSettings.getMaxTestTimeMillis();
        boolean messagesNotOver = clientSettings.getMaxTestMessages() == 0
                || experimentCounters.getMessageCounter()
                    < clientSettings.getMaxTestMessages();
        boolean connectionsNotOver = clientSettings.getMaxTestConnections() == 0
                || experimentCounters.getConnectionAttemptsCounter()
                    < clientSettings.getMaxTestConnections();
        return timeNotOver && messagesNotOver && connectionsNotOver;
    }

    @Override
    public final boolean shouldIncrementLoad(long lastIncrementTime) {
        long timeSinceIncrement = System.currentTimeMillis()
                - lastIncrementTime;
        return timeSinceIncrement >= clientSettings
                .getClientIncrementPauseSeconds() * MILLIS_PER_SECOND;
    }

}
