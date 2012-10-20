/**
 * Copyright 2012 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.monitor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class does not enforce starting or stopping once and only once without a reset.
 */
public class BasicStopwatch implements Stopwatch {
    private final Timer timer;
    private AtomicLong startTime = new AtomicLong(0L);
    private AtomicLong endTime = new AtomicLong(0L);

    /** Create a new stopwatch with no associated timer. */
    public BasicStopwatch() {
        this(null);
    }

    /**
     * Create a new stopwatch with no associated timer.
     *
     * @param timer  associated timer to record the duration when stopped
     */
    public BasicStopwatch(Timer timer) {
        this.timer = timer;
    }

    /** {@inheritDoc} */
    @Override
    public void start() {
        startTime.set(System.nanoTime());
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        endTime.set(System.nanoTime());
        if (timer != null) {
            timer.record(getDuration(timer.getTimeUnit()));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        startTime = new AtomicLong(0L);
        endTime = new AtomicLong(0L);
    }

    /** {@inheritDoc} */
    @Override
    public long getDuration(TimeUnit timeUnit) {
        return timeUnit.convert(getDuration(), TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the duration in nanoseconds. No checks are performed to ensure that the stopwatch
     * has been properly started and stopped before executing this method.
     */
    @Override
    public long getDuration() {
        return endTime.get() - startTime.get();
    }
}
