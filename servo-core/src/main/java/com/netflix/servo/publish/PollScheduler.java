/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 - 2012 Netflix
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.netflix.servo.publish;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Basic scheduler for polling metrics and reporting them to observers. You
 * can add {@link PollRunnable} objects but not remove them individually.
 * If you stop the instance and then start it again all of the prior tasks
 * will be thrown away.
 */
public class PollScheduler {
    private static final PollScheduler INSTANCE = new PollScheduler();

    /** Return the instance of this scheduler. */
    public static PollScheduler getInstance() {
        return INSTANCE;
    }

    private PollScheduler() {
    }

    private ScheduledExecutorService executor = null;
    private boolean started = false;

    /**
     * Add a tasks to execute at a fixed rate based on the provided delay.
     */
    public void addPoller(PollRunnable task, long delay, TimeUnit timeUnit) {
        if (started) {
            executor.scheduleAtFixedRate(task, 0, delay, timeUnit);
        } else {
            throw new IllegalStateException(
                "you must start the scheduler before tasks can be submitted");
        }
    }

    /**
     * Start scheduling tasks with a default thread pool, sized based on the
     * number of available processors.
     */
    public void start() {
        int numThreads = Runtime.getRuntime().availableProcessors();
        ThreadFactory factory = new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("ServoPollScheduler-%d")
            .build();
        start(Executors.newScheduledThreadPool(numThreads, factory));
    }

    /**
     * Start the poller with the given executor service.
     */
    public synchronized void start(ScheduledExecutorService service) {
        if (!started) {
            executor = service;
            started = true;
        } else {
            throw new IllegalStateException(
                "cannot start scheduler again without stopping it");
        }
    }

    /**
     * Stop the poller, shutting down the current executor service.
     */
    public synchronized void stop() {
        if (started) {
            executor.shutdown();
            started = false;
        } else {
            throw new IllegalStateException(
                "scheduler must be started before you stop it");
        }
    }

    /** Returns true if this scheduler is currently started. */
    public synchronized boolean isStarted(){
        return started;
    }
}
