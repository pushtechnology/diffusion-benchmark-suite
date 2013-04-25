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
package util;

import java.util.concurrent.atomic.AtomicLong;

public class IntervalCounterMonitor implements Runnable {
    private final AtomicLong counter;
    private final String message;
    private volatile boolean running = true;

    public IntervalCounterMonitor(final AtomicLong counter, final String message) {
        this.counter = counter;
        this.message = message;
    }

    public void halt() {
        running = false;
    }

    @Override
    public void run() {
        long lastTimeStampNanos = System.nanoTime();
        long lastMessageCount = 0;

        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                continue;
            }

            long newMessageCount = counter.get();
            long newTimeStamp = System.nanoTime();

            long numMessages = newMessageCount - lastMessageCount;
            long intervalNanos = newTimeStamp - lastTimeStampNanos;

            System.out.format("%,d %s in %,dns\n", numMessages, message,
                    intervalNanos);

            lastMessageCount = newMessageCount;
            lastTimeStampNanos = newTimeStamp;
        }
    }
}
