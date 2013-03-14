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
 * Gauge that keeps track of the maximum value seen since the last reset. Updates should be
 * non-negative, the reset value is 0.
 */
public class MaxGauge extends AbstractMonitor<Long>
        implements Gauge<Long>, ResettableMonitor<Long> {
    private final AtomicLong max = new AtomicLong(0L);

    /** Creates a new instance of the gauge. */
    public MaxGauge(MonitorConfig config) {
        super(config.withAdditionalTag(DataSourceType.GAUGE));
    }

    /** Update the max if the provided value is larger than the current max. */
    public void update(long v) {
        long currentMaxValue = max.get();
        while (v > currentMaxValue) {
            if (max.compareAndSet(currentMaxValue, v)) {
                break;
            }
            currentMaxValue = max.get();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Long getValue() {
        return max.get();
    }

    /** {@inheritDoc} */
    @Override
    public Long getAndResetValue() {
        return max.getAndSet(0L);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MaxGauge)) {
            return false;
        }
        MaxGauge m = (MaxGauge) obj;
        return config.equals(m.getConfig()) && max.get() == m.max.get();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(config, max.get());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("config", config)
                .add("max", max.get())
                .toString();
    }
}
