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

import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@link Gauge} that reports a long value.
 */
public class LongGauge implements Gauge<Long> {
    private final AtomicLong number = new AtomicLong(0L);
    private final MonitorConfig config;

    /**
     * Create a new instance with the specified configuration.
     *
     * @param config   configuration for this gauge
     */
    public LongGauge(MonitorConfig config) {
        this.config = config.withAdditionalTag(DataSourceType.GAUGE);
    }

    /**
     * Set the current value.
     */
    public void set(Long n) {
        number.set(n);
    }

    /**
     * Atomically increments by one the current value.
     * @return the updated value
     */
    public long incrementAndGet() {
        return number.incrementAndGet();
    }

    /**
     * Atomically adds the given value to the current value.
     * @return the updated value
     */
    public long addAndGet(long delta) {
        return number.addAndGet(delta);
    }

    /**
     * Atomically decrements by one the current value.
     * @return the updated value
     */
    public long decrementAndGet() {
        return number.decrementAndGet();
    }

    /**
     * Atomically increments by one the current value.
     * @return the previous value
     */
    public long getAndIncrement() {
        return number.getAndIncrement();
    }

    /**
     * Atomically adds the given value to the current value.
     * @return the previous value
     */
    public long getAndAdd(long delta) {
        return number.getAndAdd(delta);
    }

    /**
     * Atomically decrements by one the current value.
     * @return the previous value
     */
    public long getAndDecrement() {
        return number.getAndDecrement();
    }

    /**
     * Atomically sets to the given value and returns the old value.
     */
    public long getAndSet(long newValue) {
        return number.getAndSet(newValue);
    }

    /** {@inheritDoc} */
    @Override
    public Long getValue() {
        return number.get();
    }

    /** {@inheritDoc} */
    @Override
    public MonitorConfig getConfig() {
        return config;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof LongGauge)) {
            return false;
        }

        LongGauge that = (LongGauge) o;
        return config.equals(that.config) && getValue().equals(that.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(getValue(), config);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).
                add("number", number).
                add("config", config).
                toString();
    }
}
