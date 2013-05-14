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
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class PeakRateCounter extends AbstractMonitor<Long>
        implements Counter, ResettableMonitor<Long> {

    private final AtomicReference<ConcurrentHashMap<AtomicLong, AtomicLong>> counterBuckets;
    private final AtomicLong intervalTimestamp;

    public PeakRateCounter(MonitorConfig config) {
        // This class will reset the value so it is not a monotonically increasing value as
        // expected for type=COUNTER. This class looks like a counter to the user and a gauge to
        // the publishing pipeline receiving the value.
        super(config.withAdditionalTag(DataSourceType.GAUGE));

        counterBuckets = new AtomicReference<ConcurrentHashMap<AtomicLong, AtomicLong>>(new ConcurrentHashMap<AtomicLong, AtomicLong>());

        this.intervalTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    static class BucketValueComparator implements Comparator<Map.Entry<AtomicLong, AtomicLong>>, Serializable {

        @Override
        public int compare(Map.Entry<AtomicLong, AtomicLong> o1, Map.Entry<AtomicLong, AtomicLong> o2) {
            long v1 = o1.getValue().get();
            long v2 = o1.getValue().get();
            return (v1 == v2 ? 0 : v1 > v2 ? 1 : -1);
        }
    }

    @Override
    public Long getValue() {

        Set<Map.Entry<AtomicLong, AtomicLong>> entrySet = counterBuckets.get().entrySet();

        if (entrySet.isEmpty()) {
            return 0L;
        }

        Comparator<Map.Entry<AtomicLong, AtomicLong>> cmp = new BucketValueComparator();

        Map.Entry<AtomicLong, AtomicLong> max = Collections.max(entrySet, cmp);

        long maxCountPerSecond = max.getValue().get();

        return maxCountPerSecond;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment() {
        increment(1L);
    }

    /* !KS change increment to safely update when count is null
     * see DCL or Lay initialization pg 348 Goetz
     * 
     * todo keep only the current and the max
     * 
     * handle the ellapsed time wrapping for the bucketKey
     */
    /**
     * {@inheritDoc}
     */
    @Override
    public void increment(long amount) {
        long now = System.currentTimeMillis();
        long ellapsedTime = now - intervalTimestamp.get();
        AtomicLong bucketKey = new AtomicLong(TimeUnit.SECONDS.convert(ellapsedTime, TimeUnit.MILLISECONDS));
        AtomicLong count = counterBuckets.get().get(bucketKey);
        if (count != null) {
            count.addAndGet(amount);
        } else {
            count = new AtomicLong(amount);
            counterBuckets.get().put(count, count);
        }
        // get the max bucket
        // remove all but the current and max buckets
        //lock the counterBuckets while removing
        // be sure covers case where current is the max
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getAndResetValue() {
        Long value = getValue();
        counterBuckets.getAndSet(new ConcurrentHashMap<AtomicLong, AtomicLong>());
        return value;
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
                .add("peak Rate per Second", getValue())
                .toString();
    }
}