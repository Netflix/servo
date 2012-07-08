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

public interface Stopwatch {

    /**
     * Start the stopwatch.
     */
    public void start();

    /**
     * Stop the stopwatch.
     */
    public void stop();

    /**
     * Reset the stopwatch so that it can be used again.
     */
    public void reset();

    /**
     * Get the duration of time the stopwatch was running.
     * @param timeUnit
     * @return duration in specified time unit.
     */
    public long getDuration(TimeUnit timeUnit);

    /**
     * Get the duration in the default TimeUnit which is nano-seconds.
     * @return
     */
    public long getDuration();
}
