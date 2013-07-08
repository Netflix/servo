/**
 * Copyright 2013 Netflix, Inc.
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class does not enforce starting or stopping once and only once without a reset.
 */
public class BasicStopwatch implements Stopwatch {
    private final AtomicLong startTime = new AtomicLong(0L);
    private final AtomicLong endTime = new AtomicLong(0L);
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Create a new stopwatch with no associated timer. */
    public BasicStopwatch() {
    }

    /** {@inheritDoc} */
    @Override
    public void start() {
        startTime.set(System.nanoTime());
        running.set(true);
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        endTime.set(System.nanoTime());
        running.set(false);
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        startTime.set(0L);
        endTime.set(0L);
        running.set(false);
    }

    /** {@inheritDoc} */
    @Override
    public long getDuration(TimeUnit timeUnit) {
        return timeUnit.convert(getDuration(), TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the duration in nanoseconds. No checks are performed to ensure that the stopwatch
     * has been properly started and stopped before executing this method. If called before stop
     * it will return the current duration.
     */
    @Override
    public long getDuration() {
        final long end = running.get() ? System.nanoTime() : endTime.get();
        return end - startTime.get();
    }
}
