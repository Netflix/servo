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
 * Gauge that keeps track of the minimum value seen since the last reset. The reset value is
 * Long.MAX_VALUE. If no update has been received since the last reset, then {@link #getValue}
 * will return 0.
 */
public class MinGauge extends AbstractMonitor<Long>
        implements Gauge<Long>, ResettableMonitor<Long> {
    private final AtomicLongArray min = new AtomicLongArray(Pollers.NUM_POLLERS);

    /**
     * Creates a new instance of the gauge.
     */
    public MinGauge(MonitorConfig config) {
        super(config.withAdditionalTag(DataSourceType.GAUGE));
        for (int i = 0; i < min.length(); ++i) {
            min.set(i, Long.MAX_VALUE);
        }
    }

    private void updateMin(int idx, long v) {
        long currentMinValue = min.get(idx);
        while (v < currentMinValue) {
            if (min.compareAndSet(idx, currentMinValue, v)) {
                break;
            }
            currentMinValue = min.get(idx);
        }
    }

    /**
     * Update the min if the provided value is smaller than the current min.
     */
    public void update(long v) {
        for (int i = 0; i < min.length(); ++i) {
            updateMin(i, v);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getValue() {
        long v = min.get(0);
        return (v == Long.MAX_VALUE) ? 0L : v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getAndResetValue() {
        return getAndResetValue(0);
    }

    @Override
    public Long getAndResetValue(int pollerIdx) {
        long v = min.getAndSet(pollerIdx, Long.MAX_VALUE);
        return (v == Long.MAX_VALUE) ? 0L : v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MinGauge)) {
            return false;
        }
        MinGauge m = (MinGauge) obj;
        return config.equals(m.getConfig()) && AtomicUtils.equals(min, m.min);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(config, AtomicUtils.hashCode(min));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("config", config)
                .add("min", min)
                .toString();
    }
}
