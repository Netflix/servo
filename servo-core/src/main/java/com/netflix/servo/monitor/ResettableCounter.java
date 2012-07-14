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

import com.netflix.servo.annotations.DataSourceType;

import com.google.common.base.Objects;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Counter implementation that keeps track of updates since the last reset.
 */
public class ResettableCounter extends AbstractMonitor<Long>
        implements Counter, ResettableMonitor<Long> {
    protected final AtomicLong count = new AtomicLong(0L);

    /** Create a new instance of the counter. */
    public ResettableCounter(MonitorConfig config) {
        // This class will reset the value so it is not a monotonically increasing value as
        // expected for type=COUNTER. This class looks like a counter to the user and a gauge to
        // the publishing pipeline receiving the value.
        super(config.withAdditionalTag(DataSourceType.GAUGE));
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
    public Long getValue() {
        return count.get();
    }

    /** {@inheritDoc} */
    @Override
    public Long getAndResetValue() {
        return count.getAndSet(0L);
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
