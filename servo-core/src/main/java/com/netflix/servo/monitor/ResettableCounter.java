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

import com.google.common.base.Objects;
import com.netflix.servo.annotations.DataSourceType;

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Counter implementation that keeps track of updates since the last reset.
 */
public class ResettableCounter extends AbstractMonitor<Number>
        implements Counter, ResettableMonitor<Number> {
    private final long[] estPollingInterval;

    private final AtomicLongArray count;
    private final AtomicLongArray lastResetTime;

    /**
     * Create a new instance of the counter.
     *
     * @param config           configuration for the monitor
     * @param pollingIntervals polling intervals in milliseconds.
     */
    ResettableCounter(MonitorConfig config, long[] pollingIntervals) {
        // This class will reset the value so it is not a monotonically increasing value as
        // expected for type=COUNTER. This class looks like a counter to the user and a rate to
        // the publishing pipeline receiving the value.
        super(config.withAdditionalTag(DataSourceType.RATE));
        count = new AtomicLongArray(pollingIntervals.length);
        lastResetTime = new AtomicLongArray(pollingIntervals.length);
        this.estPollingInterval = new long[pollingIntervals.length];
        long now = System.currentTimeMillis();
        for (int i = 0; i < pollingIntervals.length; ++i) {
            this.estPollingInterval[i] = pollingIntervals[i];
            if (estPollingInterval[i] > 0) {
                lastResetTime.set(i, -1L);
            } else {
                lastResetTime.set(i, now);
            }
        }
    }

    /** Create a new instance of the counter. */
    public ResettableCounter(MonitorConfig config) {
        this(config, Pollers.POLLING_INTERVALS);
    }

    /**
     * Create a new instance of the counter.
     *
     * @param config              configuration for the monitor
     * @param estPollingInterval  ignored
     * @deprecated Polling intervals are configured via the system wide property servo.pollers instead
     *
     */
    @Deprecated
    public ResettableCounter(MonitorConfig config, long estPollingInterval) {
        this(config, Pollers.POLLING_INTERVALS);
    }

    /** {@inheritDoc} */
    @Override
    public void increment() {
        for (int i = 0; i < count.length(); ++i) {
            count.getAndIncrement(i);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void increment(long amount) {
        for (int i = 0; i < count.length(); ++i) {
            count.getAndAdd(i, amount);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Number getValue() {
        final long now = System.currentTimeMillis();
        return computeRate(now, lastResetTime.get(0), count.get(0), estPollingInterval[0]);
    }

    /** {@inheritDoc} */
    @Override
    public Number getAndResetValue() {
        return getAndResetValue(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Number getAndResetValue(int pollerIdx) {
        final long now = System.currentTimeMillis();
        final long lastReset = lastResetTime.getAndSet(pollerIdx, now);
        final long currentCount = count.getAndSet(pollerIdx, 0L);
        return computeRate(now, lastReset, currentCount, estPollingInterval[pollerIdx]);
    }

    /** Returns the raw count instead of the rate. This can be useful for unit tests. */
    public long getCount() {
        return count.get(0);
    }

    /** Returns the rate per second since the last reset. */
    private double computeRate(long now, long lastReset, long currentCount, long pollingInterval) {
        final double delta = ((lastReset >= 0) ? (now - lastReset) : pollingInterval) / 1000.0;
        return (currentCount < 0 || delta <= 0.0) ? 0.0 : currentCount / delta;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ResettableCounter)) {
            return false;
        }
        ResettableCounter m = (ResettableCounter) obj;
        return config.equals(m.getConfig()) && AtomicUtils.equals(count, m.count);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(config, AtomicUtils.hashCode(count));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("config", config)
                .add("count", count)
                .add("resets", lastResetTime)
                .toString();
    }
}
