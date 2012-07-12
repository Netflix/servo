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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.Tag;

import java.util.List;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

/**
 * A simple timer implementation providing the total time, count, min, and max for the times that
 * have been recorded.
 */
public class BasicTimer extends AbstractMonitor<Long> implements Timer, CompositeMonitor<Long> {

    private static final String STATISTIC = "statistic";
    private static final String UNIT = "unit";

    private static final Tag STAT_TOTAL = new BasicTag(STATISTIC, "totalTime");
    private static final Tag STAT_COUNT = new BasicTag(STATISTIC, "count");
    private static final Tag STAT_MIN = new BasicTag(STATISTIC, "min");
    private static final Tag STAT_MAX = new BasicTag(STATISTIC, "max");

    private final TimeUnit timeUnit;

    private final Counter totalTime;
    private final Counter count;
    private final MinGauge min;
    private final MaxGauge max;

    private final List<Monitor<?>> monitors;

    /**
     * Creates a new instance of the timer with a unit of milliseconds.
     */
    public BasicTimer(MonitorConfig config) {
        this(config, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new instance of the timer.
     */
    public BasicTimer(MonitorConfig config, TimeUnit unit) {
        super(config);

        final Tag unitTag = new BasicTag(UNIT, unit.name());
        final MonitorConfig unitConfig = config.withAdditionalTag(unitTag);
        timeUnit = unit;

        totalTime = new BasicCounter(unitConfig.withAdditionalTag(STAT_TOTAL));
        count = new BasicCounter(unitConfig.withAdditionalTag(STAT_COUNT));
        min = new MinGauge(unitConfig.withAdditionalTag(STAT_MIN));
        max = new MaxGauge(unitConfig.withAdditionalTag(STAT_MAX));
        monitors = ImmutableList.<Monitor<?>>of(totalTime, count, min, max);
    }

    /** {@inheritDoc} */
    @Override
    public List<Monitor<?>> getMonitors() {
        return monitors;
    }

    /** {@inheritDoc} */
    @Override
    public Stopwatch start() {
        Stopwatch s = new BasicStopwatch(this);
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
    public void record(long duration) {
        totalTime.increment(duration);
        count.increment();
        min.update(duration);
        max.update(duration);
    }

    /** {@inheritDoc} */
    @Override
    public void record(long duration, TimeUnit timeUnit) {
        record(this.timeUnit.convert(duration, timeUnit));
    }

    /** {@inheritDoc} */
    @Override
    public Long getValue() {
        final long cnt = count.getValue();
        return (cnt == 0) ? 0L : totalTime.getValue() / cnt;
    }

    /** Get the total time for all updates since the last reset. */
    public Long getTotalTime() {
        return totalTime.getValue();
    }

    /** Get the number of updates since the last reset. */
    public Long getCount() {
        return count.getValue();
    }

    /** Get the min value since the last reset. */
    public Long getMin() {
        return min.getValue();
    }

    /** Get the max value since the last reset. */
    public Long getMax() {
        return max.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof BasicTimer)) {
            return false;
        }
        BasicTimer m = (BasicTimer) obj;
        return config.equals(m.getConfig())
            && totalTime.equals(m.totalTime)
            && count.equals(m.count)
            && min.equals(m.min)
            && max.equals(m.max);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(config, totalTime, count, min, max);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("config", config)
                .add("totalTime", totalTime)
                .add("count", count)
                .add("min", min)
                .add("max", max)
                .toString();
    }
}
