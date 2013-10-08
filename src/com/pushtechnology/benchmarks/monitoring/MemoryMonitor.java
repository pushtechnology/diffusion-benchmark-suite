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
package com.pushtechnology.benchmarks.monitoring;

/**
 * @author nitsanw
 * 
 */
public interface MemoryMonitor {

    /**
     * @return committed heap in bytes
     */
    long heapCommitted();

    /**
     * @return used heap in bytes
     */
    long heapUsed();

    /**
     * @return max heap in bytes
     */
    long heapMax();

    /**
     * @return committed off heap in bytes
     */
    long offHeapCommitted();

    /**
     * @return used off heap in bytes
     */
    long offHeapUsed();

    /**
     * @return max off heap in bytes
     */
    long offHeapMax();

    /**
     * Take a current sample of memory use.
     */
    void sample();

}
