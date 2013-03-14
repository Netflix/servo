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
 * Gauge that keeps track of the minimum value seen since the last reset. The reset value is
 * Long.MAX_VALUE. If no update has been received since the last reset, then {@link #getValue}
 * will return 0.
 */
public class MinGauge extends AbstractMonitor<Long>
        implements Gauge<Long>, ResettableMonitor<Long> {
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);

    /** Creates a new instance of the gauge. */
    public MinGauge(MonitorConfig config) {
        super(config.withAdditionalTag(DataSourceType.GAUGE));
    }

    /** Update the min if the provided value is smaller than the current min. */
    public void update(long v) {
        long currentMinValue = min.get();
        while (v < currentMinValue) {
            if (min.compareAndSet(currentMinValue, v)) {
                break;
            }
            currentMinValue = min.get();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Long getValue() {
        long v = min.get();
        return (v == Long.MAX_VALUE) ? 0L : v;
    }

    /** {@inheritDoc} */
    @Override
    public Long getAndResetValue() {
        long v = min.getAndSet(Long.MAX_VALUE);
        return (v == Long.MAX_VALUE) ? 0L : v;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MinGauge)) {
            return false;
        }
        MinGauge m = (MinGauge) obj;
        return config.equals(m.getConfig()) && min.get() == m.min.get();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(config, min.get());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("config", config)
                .add("min", min.get())
                .toString();
    }
}
