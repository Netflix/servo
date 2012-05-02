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

package com.netflix.servo.monitor;

import java.util.concurrent.TimeUnit;

public interface Stopwatch {

    /**
     * Creates a new Stopwatch that has not been started.
     * @return The new Stopwatch
     */
    public Stopwatch createStopwatch();

    /**
     * Creates a new Stopwatch instance.  allowing the caller to specify whether or not is started right away.
     * @param started
     * @return The new Stopwatch
     */
    public Stopwatch createStopwatch(boolean started);

    /**
     * Start the stopwatch.
     */
    public void start();

    /**
     * Stop the stopwatch.
     */
    public void stop();

    /**
     * Get the duration of time the stopwatch was running.
     * @param timeUnit
     * @return duration in specified time unit.
     */
    public double getDuration(TimeUnit timeUnit);
}
