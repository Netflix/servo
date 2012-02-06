/*
 * Copyright (c) 2012. Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.netflix.servo.publish;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Very basic timed poller singleton.  You can add PollRunnables but not remove them.
 * If you stop the instance and then start it again all of the prior PollRunnables will be thrown away.
 */
public class TimedMetricPoller {
    private static TimedMetricPoller ourInstance = new TimedMetricPoller();

    public static TimedMetricPoller getInstance() {
        return ourInstance;
    }

    private TimedMetricPoller() {
    }
    
    private ScheduledExecutorService scheduledExecutorService;
    private boolean started = false;

    /**
     * Add a PollRunnable to execute with a fixed delay from when the last execution finishes.
     * @param poller
     * @param delay
     * @param timeUnit
     */
    public void addPoller(PollRunnable poller, long delay, TimeUnit timeUnit){
        if(started){
            scheduledExecutorService.scheduleWithFixedDelay(poller, 0, delay, timeUnit);
        } else {
            throw new IllegalStateException("You must start the poller before you can add things.");
        }
    }

    /**
     * Start the poller with a scheduled threadpool of size 5.
     */
    public void start(){
        //TODO This should use a daemon ThreadFactory
        start(Executors.newScheduledThreadPool(5));
    }

    /**
     * Start the poller with the give executor service.
     * @param service
     */
    public synchronized void start(ScheduledExecutorService service){
        if(!started){
            scheduledExecutorService = service;
            started = true;
        } else {
            throw new IllegalStateException("Cannot start poller again without stopping it.");
        }
    }

    /**
     * Stop the poller, shutting down the current executor service.
     */
    public synchronized void stop(){
        if(started){
        scheduledExecutorService.shutdown();
        started = false;
        } else {
            throw new IllegalStateException("Poller must be started before you stop it.");
        }
    }

    public synchronized boolean isStarted(){
        return started;
    }
}
