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

import com.netflix.servo.annotations.DataSourceType;

import com.google.common.base.Objects;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Counter implementation that keeps track of updates since the last reset.
 */
public class ResettableCounter extends AbstractMonitor<Number>
        implements Counter, ResettableMonitor<Number> {
    private final long estPollingInterval;

    private final AtomicLong count = new AtomicLong(0L);

    private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());

    /** Create a new instance of the counter. */
    public ResettableCounter(MonitorConfig config) {
        this(config, 0L);
    }

    /**
     * Create a new instance of the counter.
     *
     * @param config              configuration for the monitor
     * @param estPollingInterval  estimated polling interval in milliseconds to use for the first
     *                            call. If this is set to 0 the time delta for the first call to
     *                            getAndResetValue will be calculated based on the creation time
     *                            which can result in overweighting the values if counters are
     *                            dynamically created during the middle of a polling interval.
     */
    public ResettableCounter(MonitorConfig config, long estPollingInterval) {
        // This class will reset the value so it is not a monotonically increasing value as
        // expected for type=COUNTER. This class looks like a counter to the user and a gauge to
        // the publishing pipeline receiving the value.
        super(config.withAdditionalTag(DataSourceType.GAUGE));
        this.estPollingInterval = estPollingInterval;
        if (estPollingInterval > 0) {
            lastResetTime.set(-1L);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void increment() {
        count.incrementAndGet();
    }

    /** {@inheritDoc} */
    @Override
    public void increment(long amount) {
        count.getAndAdd(amount);
    }

    /** {@inheritDoc} */
    @Override
    public Number getValue() {
        final long now = System.currentTimeMillis();
        return computeRate(now, lastResetTime.get(), count.get());
    }

    /** {@inheritDoc} */
    @Override
    public Number getAndResetValue() {
        final long now = System.currentTimeMillis();
        final long lastReset = lastResetTime.getAndSet(now);
        final long currentCount = count.getAndSet(0L);
        return computeRate(now, lastReset, currentCount);
    }

    /** Returns the raw count instead of the rate. This can be useful for unit tests. */
    public long getCount() {
        return count.get();
    }

    /** Returns the rate per second since the last reset. */
    private double computeRate(long now, long lastReset, long currentCount) {
        final double delta = ((lastReset >= 0) ? (now - lastReset) : estPollingInterval) / 1000.0;
        return (currentCount < 0 || delta <= 0.0) ? 0.0 : currentCount / delta;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ResettableCounter)) {
            return false;
        }
        ResettableCounter m = (ResettableCounter) obj;
        return config.equals(m.getConfig()) && count.get() == m.count.get();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(config, count.get());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("config", config)
                .add("count", count.get())
                .toString();
    }
}
