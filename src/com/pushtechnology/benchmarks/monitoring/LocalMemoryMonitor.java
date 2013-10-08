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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Local memory monitoring utility.
 * 
 * @author nitsanw
 * 
 */
public final class LocalMemoryMonitor implements MemoryMonitor {
    // CHECKSTYLE:OFF
    private final MemoryMXBean memoryMXBean;
    private MemoryUsage currentMemoryUsage;
    private MemoryUsage currentOffHeapMemoryUsage;

    /**
     * Constructor for local memory monitor.
     */
    public LocalMemoryMonitor() {
        // CHECKSTYLE:ON
        memoryMXBean = ManagementFactory.getMemoryMXBean();
    }

    @Override
    public long heapCommitted() {
        return getHeapMemoryUsage().getCommitted();
    }

    @Override
    public long heapUsed() {
        return getHeapMemoryUsage().getUsed();
    }

    @Override
    public long heapMax() {
        return getHeapMemoryUsage().getMax();
    }

    @Override
    public long offHeapCommitted() {
        return getOffHeapMemoryUsage().getCommitted();
    }

    @Override
    public long offHeapUsed() {
        return getOffHeapMemoryUsage().getUsed();
    }

    @Override
    public long offHeapMax() {
        return getOffHeapMemoryUsage().getMax();
    }

    /**
     * @return current heap memory use
     */
    private MemoryUsage getHeapMemoryUsage() {
        return currentMemoryUsage;
    }

    /**
     * @return current heap memory use
     */
    private MemoryUsage getOffHeapMemoryUsage() {
        return currentOffHeapMemoryUsage;
    }

    @Override
    public void sample() {
        currentMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        currentOffHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
    }
}
