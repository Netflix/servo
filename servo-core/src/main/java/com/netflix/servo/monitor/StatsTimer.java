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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Objects;
import com.netflix.servo.stats.StatsConfig;
import com.netflix.servo.tag.Tags;

/**
 * A {@link Timer} that provides statistics.
 * <p>
 * The statistics are collected periodically and are published according to the configuration
 * specified by the user using a {@link com.netflix.servo.stats.StatsConfig} object.
 */
public class StatsTimer extends StatsMonitor implements Timer {


    private final TimeUnit timeUnit;
    private static final String UNIT = "unit";

    /**
     * Creates a new instance of the timer with a unit of milliseconds, using the default executor.
     */
    public StatsTimer(MonitorConfig baseConfig, StatsConfig statsConfig) {
        this(baseConfig, statsConfig, TimeUnit.MILLISECONDS, defaultExecutor);
    }

    /**
     * Creates a new instance of the timer with a given unit, using the default executor.
     */
    public StatsTimer(MonitorConfig baseConfig, StatsConfig statsConfig, TimeUnit unit) {
        this(baseConfig, statsConfig, unit, defaultExecutor);
    }


    /**
     * Creates a new instance of the timer with a unit of milliseconds, using the {@link ScheduledExecutorService} provided by
     * the user.
     */
    public StatsTimer(MonitorConfig config, StatsConfig statsConfig, TimeUnit unit, ScheduledExecutorService executor) {
        super(config, statsConfig, executor, "totalTime", false, Tags.newTag(UNIT, unit.name()));
        this.timeUnit = unit;
        startComputingStats();
    }

    /** {@inheritDoc} */
    @Override
    public Stopwatch start() {
        Stopwatch s = new TimedStopwatch(this);
        s.start();
        return s;
    }

    /** {@inheritDoc} */
    @Override
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /** {@inheritDoc} */
    @Override
    public void record(long duration, TimeUnit timeUnit) {
        record(this.timeUnit.convert(duration, timeUnit));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
        		.add("StatsMonitor", super.toString())
                .add("timeUnit", timeUnit)
                .toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
    	if (!(obj instanceof StatsTimer)){ return false; }
        final StatsTimer m = (StatsTimer) obj;
        return super.equals(obj) && timeUnit.equals(m.timeUnit);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), timeUnit);
    }

    /**
     * Get the number of times this timer has been updated
     */
    public long getCount() {
        return count.getValue().longValue();
    }


    /**
     * Get the total time recorded for this timer
     */
    public long getTotalTime() {
        return getTotalMeasurement();
    }
}
