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

import monitoring.ExperimentCounters;

public final class PushBackLoadStrategy implements ExperimentLoadStrategy {


    private final ExperimentCounters experimentCounters;
    private final CommonClientSettings clientSettings;

    private long messagesDuringInterval = 0;
    private long lastAvgMessagesSeen = -1;
    private long avgMessagesSeen = 0;
    private int avgPopSize = 0;

    public PushBackLoadStrategy(ExperimentCounters experimentCountersP,
            CommonClientSettings clientSettingsP) {
        super();
        this.experimentCounters = experimentCountersP;
        this.clientSettings = clientSettingsP;
    }
    
    @Override
    public boolean shouldIncrementLoad(long lastIncrementTime) {
        long timeSinceIncrement = System.currentTimeMillis()
                - lastIncrementTime;
        if (timeSinceIncrement > 1000) {
            // get reading from monitor so that results are consistent
            messagesDuringInterval = 
                    experimentCounters.lastMessagesPerSecond.get();
            // exponential moving avg(alpha=0.1) last elements
            if (avgMessagesSeen != 0) {
                avgMessagesSeen = (long) (0.1 * messagesDuringInterval
                        + 0.9 * avgMessagesSeen);
                avgPopSize++;
            } else {
                avgMessagesSeen = messagesDuringInterval;
                avgPopSize = 1;
            }
        }
        // increase load as long as server is coping --> we receive more
        // messages as we add more clients
        boolean areResultsStableEnough = messagesDuringInterval != 0
                && avgMessagesSeen > lastAvgMessagesSeen && avgPopSize > 10;
        if (areResultsStableEnough) {
            System.out.println("incrementing:" + avgMessagesSeen + " > "
                    + lastAvgMessagesSeen);
            lastAvgMessagesSeen = avgMessagesSeen;
            avgMessagesSeen = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean testNotOver(long testStartTime) {
        long timeSinceStart =
                System.currentTimeMillis() - testStartTime;
        long maxTestTimeMillis =
                clientSettings.getMaxTestTimeMillis();
        return timeSinceStart < maxTestTimeMillis;

    }
}
