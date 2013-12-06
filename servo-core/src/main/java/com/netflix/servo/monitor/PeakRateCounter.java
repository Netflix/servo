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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A resettable counter. The value is the maximum count per second within the specified interval
 * until the counter is reset.
 */
public class PeakRateCounter extends AbstractMonitor<Number>
        implements Counter, ResettableMonitor<Number> {

    private static class PeakInterval {
        final AtomicReference<AtomicLongArray> countsRef;

        PeakInterval(int numBuckets) {
            countsRef = new AtomicReference<AtomicLongArray>(new AtomicLongArray(numBuckets));
        }

        Number getValue() {
            return getValue(countsRef.get());
        }

        Number getValue(AtomicLongArray counts) {
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

        Number getAndResetValue() {
            AtomicLongArray counts = countsRef.getAndSet(new AtomicLongArray(countsRef.get().length()));
            return getValue(counts);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PeakInterval that = (PeakInterval) o;
            return AtomicUtils.equals(countsRef.get(), that.countsRef.get());
        }

        @Override
        public int hashCode() {
            return AtomicUtils.hashCode(countsRef.get());
        }

        void increment(long now, long amount) {
            int index = (int) now % countsRef.get().length();
            countsRef.get().addAndGet(index, amount);
        }
    }

    private final PeakInterval[] peakIntervals;

    public PeakRateCounter(MonitorConfig config) {
        this(config, Pollers.POLLING_INTERVALS);
    }

    PeakRateCounter(MonitorConfig config, long[] pollingIntervals) {
        // This class will reset the value so it is not a monotonically increasing value as
        // expected for type=COUNTER. This class looks like a counter to the user and a gauge to
        // the publishing pipeline receiving the value.
        super(config.withAdditionalTag(DataSourceType.RATE));

        peakIntervals = new PeakInterval[pollingIntervals.length];
        for (int i = 0; i < peakIntervals.length; ++i) {
            peakIntervals[i] = new PeakInterval((int) (pollingIntervals[i] / 1000L));
        }
    }

    /**
     * Create a new instance with the specified interval.
     *
     * @deprecated Polling intervals are configured using the system property servo.pollers
     */
    @Deprecated
    public PeakRateCounter(MonitorConfig config, int intervalSeconds) {
        this(config, Pollers.POLLING_INTERVALS);
    }

    /** {@inheritDoc} */
    @Override
    public Number getValue() {
        return peakIntervals[0].getValue();
    }

    /** {@inheritDoc} */
    @Override
    public Number getAndResetValue() {
        return getAndResetValue(0);
    }

    /** {@inheritDoc} */
    @Override
    public Number getAndResetValue(int pollerIdx) {
        return peakIntervals[pollerIdx].getAndResetValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PeakRateCounter)) {
            return false;
        }
        PeakRateCounter c = (PeakRateCounter) obj;

        return config.equals(c.getConfig())
                && (Arrays.equals(this.peakIntervals, c.peakIntervals));
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
        for (PeakInterval peakInterval : peakIntervals) {
            peakInterval.increment(now, amount);
        }
    }
}

