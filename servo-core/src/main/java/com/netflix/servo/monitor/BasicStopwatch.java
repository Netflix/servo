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
package com.netflix.servo.monitor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class does not enforce starting or stopping once and only once without a reset.
 */
public class BasicStopwatch implements Stopwatch {
    private Timer timer;
    private AtomicLong startTime = new AtomicLong(0L);
    private AtomicLong endTime = new AtomicLong(0L);

    public BasicStopwatch() {
        this.timer = null;
    }

    public BasicStopwatch(Timer timer) {
        this.timer = timer;
    }

    /**
     * Start the stopwatch.
     */
    @Override
    public void start() {
        startTime.set(System.nanoTime());
    }

    /**
     * Stop the stopwatch.
     */
    @Override
    public void stop() {
        endTime.set(System.nanoTime());
        if (timer != null) {
            timer.record(getDuration(timer.getTimeUnit()));
        }
    }

    /**
     * Reset the stopwatch so that it can be used again.
     */
    @Override
    public void reset() {
        startTime = new AtomicLong(0L);
        endTime = new AtomicLong(0L);
    }

    /**
     * Get the duration of time the stopwatch was running.
     *
     * @param timeUnit
     * @return duration in specified time unit.
     */
    @Override
    public long getDuration(TimeUnit timeUnit) {
        return timeUnit.convert(getDuration(), TimeUnit.NANOSECONDS);
    }

    /**
     * Get the duration in the default TimeUnit which is nano-seconds.
     *
     * @return
     */
    @Override
    public long getDuration() {
        return endTime.get() - startTime.get();
    }
}
