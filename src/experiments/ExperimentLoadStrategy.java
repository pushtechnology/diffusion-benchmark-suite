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

/**
 * Used by the experiment loop to determine when load should be increased.
 * 
 * @author nitsanw
 *
 */
public interface ExperimentLoadStrategy {
    /**
     * @param lastIncrementTime ...
     * @return true if we should
     */
    boolean shouldIncrementLoad(long lastIncrementTime);
    
    /**
     * @param testStartTime ...
     * @return true if test is over
     */
    boolean testNotOver(long testStartTime);
}
