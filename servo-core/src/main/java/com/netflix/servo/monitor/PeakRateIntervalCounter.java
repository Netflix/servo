/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.netflix.servo.monitor;

import com.google.common.base.Objects;
import com.netflix.servo.annotations.DataSourceType;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A resettable counter implementation backed by an
 * {@link java.util.concurrent.atomic.AtomicLongArray}. The value is the maximum
 * count per second within the specified interval until the counter is reset.
 */
public class PeakRateIntervalCounter extends AbstractMonitor<Long>
        implements Counter, ResettableMonitor<Long> {

    private final AtomicReference<AtomicLongArray> buckets;
    private final int numBuckets;

    public PeakRateIntervalCounter(MonitorConfig config, int intervalSeconds) {
        // This class will reset the value so it is not a monotonically increasing value as
        // expected for type=COUNTER. This class looks like a counter to the user and a gauge to
        // the publishing pipeline receiving the value.
        super(config.withAdditionalTag(DataSourceType.GAUGE));
        numBuckets = intervalSeconds;
        buckets = new AtomicReference<AtomicLongArray>(new AtomicLongArray(numBuckets));
    }

    @Override
    public Long getValue() {
        AtomicLongArray counts = buckets.get();
        long max = 0;
        long cnt;

        for (int i = 0; i < counts.length(); i++) {
            cnt = counts.get(i);
            if (cnt > max) {
                max = cnt;
            }
        }
        return max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getAndResetValue() {
        Long value = getValue();
        buckets.set(new AtomicLongArray(numBuckets));
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PeakRateIntervalCounter)) {
            return false;
        }
        PeakRateIntervalCounter c = (PeakRateIntervalCounter) obj;
        long v1 = this.getValue();
        long v2 = c.getValue();
        return config.equals(c.getConfig())
                && (v1 == v2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(config, getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("config", config)
                .add("max rate per second", getValue())
                .toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment() {
        increment(1L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment(long amount) {

        long now = System.currentTimeMillis() / 1000L;
        Long bucket = now % numBuckets;
        int index = bucket.intValue();

        buckets.get().addAndGet(index, amount);
    }
}