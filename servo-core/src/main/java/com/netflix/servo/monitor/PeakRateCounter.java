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
 * A resettable counter. The value is the maximum count per second within the specified interval
 * until the counter is reset.
 */
public class PeakRateCounter extends AbstractMonitor<Number>
        implements Counter, ResettableMonitor<Number> {

    private final AtomicReference<AtomicLongArray> buckets;
    private final int numBuckets;

    /** Create a new instance with the specified interval. */
    public PeakRateCounter(MonitorConfig config, int intervalSeconds) {
        // This class will reset the value so it is not a monotonically increasing value as
        // expected for type=COUNTER. This class looks like a counter to the user and a gauge to
        // the publishing pipeline receiving the value.
        super(config.withAdditionalTag(DataSourceType.GAUGE));
        numBuckets = intervalSeconds;
        buckets = new AtomicReference<AtomicLongArray>(new AtomicLongArray(numBuckets));
    }

    /** {@inheritDoc} */
    @Override
    public Number getValue() {
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

    /** {@inheritDoc} */
    @Override
    public Number getAndResetValue() {
        Number value = getValue();
        buckets.set(new AtomicLongArray(numBuckets));
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PeakRateCounter)) {
            return false;
        }
        PeakRateCounter c = (PeakRateCounter) obj;

        return config.equals(c.getConfig())
                && (this.getValue() == c.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(config, getValue());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("config", config)
                .add("max rate per second", getValue())
                .toString();
    }

    /** {@inheritDoc} */
    @Override
    public void increment() {
        increment(1L);
    }

    /** {@inheritDoc} */
    @Override
    public void increment(long amount) {

        long now = System.currentTimeMillis() / 1000L;
        int index = (int) now % numBuckets;

        buckets.get().addAndGet(index, amount);
    }
}

