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
package com.pushtechnology.benchmarks.publishers;

import java.util.concurrent.atomic.AtomicLong;

public final class PublisherAssembly {
    private final AtomicLong messageCounter = new AtomicLong();
    private final AtomicLong topicsCounter = new AtomicLong();
    private final MessagePublisher messagePublisher;
    private final BroadcastConfiguration config;

    private Thread injectionRunnerThread;
    private BroadcastRunner injectionRunner;
    private Thread monitorThread;

    public PublisherAssembly(final MessagePublisher messagePublisher,
            final BroadcastConfiguration config) {
        this.messagePublisher = messagePublisher;
        this.config = config;
    }

    public synchronized void init() {
        if (monitorThread != null) {
            throw new IllegalStateException(
                    "Init should be called only once, found monitorThread");
        }

        injectionRunner = new BroadcastRunner(messagePublisher, messageCounter,
                topicsCounter, config);
        injectionRunnerThread = new Thread(injectionRunner);
        injectionRunnerThread.setName("injector-runner-thread");
        injectionRunnerThread.setDaemon(true);
        injectionRunnerThread.start();
    }

    public synchronized void destroy() {
        if (monitorThread == null) {
            return;
        }
        injectionRunner.halt();
        injectionRunnerThread.interrupt();
        try {
            injectionRunnerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        injectionRunnerThread = null;
    }
}
