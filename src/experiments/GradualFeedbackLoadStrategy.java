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
package experiments;

import util.RollingAverage;
import monitoring.ExperimentCounters;

/**
 * This load strategy increases load when it established that the prev.
 * increment resulted in increased throughput.
 * 
 * @author nitsanw
 * 
 */
public final class GradualFeedbackLoadStrategy implements
        ExperimentLoadStrategy {
    // CHECKSTYLE:OFF
    private static final int MIN_SAMPLE_SIZE = 10;
    private static final int MIN_INCREMENT_CHECK_TIME = 1000;
    private final ExperimentCounters experimentCounters;
    private final CommonExperimentSettings clientSettings;
    private final RollingAverage messagesPerSecondRollingAvg =
            new RollingAverage(16);
    private long lastAvgMessagesSeen;
    private long numberOfConnectionsAtSampleStart;

    // CHECKSTYLE:ON

    /**
     * @param experimentCountersP ...
     * @param clientSettingsP ...
     */
    public GradualFeedbackLoadStrategy(ExperimentCounters experimentCountersP,
            CommonExperimentSettings clientSettingsP) {
        super();
        this.experimentCounters = experimentCountersP;
        this.clientSettings = clientSettingsP;
    }

    @Override
    public boolean shouldIncrementLoad(long lastIncrementTime) {
        long currentlyConnected = experimentCounters.getCurrentlyConnected();
        long timeSinceIncrement = System.currentTimeMillis()
                - lastIncrementTime;
        if (timeSinceIncrement > MIN_INCREMENT_CHECK_TIME) {
            messagesPerSecondRollingAvg.sample(
                    experimentCounters.getLastMessagesPerSecond());
        } else {
            numberOfConnectionsAtSampleStart = currentlyConnected;
        }
        // increase load as long as server is coping --> we receive more
        // messages as we add more clients
        boolean lostConnections = currentlyConnected
                < numberOfConnectionsAtSampleStart;
        boolean rollingAvgIncreasing = messagesPerSecondRollingAvg.avg()
                > lastAvgMessagesSeen;
        boolean sufficientSample = messagesPerSecondRollingAvg.size()
                > MIN_SAMPLE_SIZE;
        boolean areResultsStableEnough = !lostConnections
                && rollingAvgIncreasing
                && sufficientSample;
        if (areResultsStableEnough) {
            lastAvgMessagesSeen = messagesPerSecondRollingAvg.avg();
            messagesPerSecondRollingAvg.reset();
            return true;
        } else if (lostConnections) {
            lastAvgMessagesSeen = 0;
            messagesPerSecondRollingAvg.reset();
        }
        return false;
    }

    @Override
    public boolean testNotOver(long testStartTime) {
        return testStartTime + clientSettings.getMaxTestTimeMillis()
        < System.currentTimeMillis();

    }
}
