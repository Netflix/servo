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

import com.google.common.base.Function;

import com.netflix.servo.tag.TaggingContext;

import java.util.concurrent.TimeUnit;

/**
 * Composite that maintains separate simple timers for each distinct set of tags returned by the
 * tagging context.
 */
public class ContextualTimer extends AbstractContextualMonitor<Long, Timer> implements Timer {

    /**
     * Create a new instance of the timer.
     *
     * @param config      shared configuration
     * @param context     provider for context specific tags
     * @param newMonitor  function to create new timers
     */
    public ContextualTimer(
            MonitorConfig config,
            TaggingContext context,
            Function<MonitorConfig, Timer> newMonitor) {
        super(config, context, newMonitor);
    }

    /** {@inheritDoc} */
    @Override
    public Stopwatch start() {
        Stopwatch s = new TimedStopwatch(getMonitorForCurrentContext());
        s.start();
        return s;
    }

    /** {@inheritDoc} */
    @Override
    public TimeUnit getTimeUnit() {
        return getMonitorForCurrentContext().getTimeUnit();
    }

    /** {@inheritDoc} */
    @Override
    public void record(long duration) {
        getMonitorForCurrentContext().record(duration);
    }

    /** {@inheritDoc} */
    @Override
    public void record(long duration, TimeUnit timeUnit) {
        getMonitorForCurrentContext().record(duration, timeUnit);
    }

    /** {@inheritDoc} */
    @Override
    public Long getValue() {
        return getMonitorForCurrentContext().getValue();
    }
}
