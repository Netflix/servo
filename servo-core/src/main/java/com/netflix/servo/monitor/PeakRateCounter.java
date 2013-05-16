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
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment() {
        increment(1L);
    }

    @Override
    public Long getValue() {
        Map.Entry<AtomicLong, AtomicLong> max = getMaxBucket();
        return (max == null ? 0L : max.getValue().get());
    }

    static class MapEntryValueComparator implements Comparator<Map.Entry<AtomicLong, AtomicLong>>, Serializable {

        @Override
        public int compare(Map.Entry<AtomicLong, AtomicLong> o1, Map.Entry<AtomicLong, AtomicLong> o2) {
            long v1 = o1.getValue().get();
            long v2 = o2.getValue().get();
            return (v1 == v2 ? 0 : v1 > v2 ? 1 : -1);
        }
    }

    Map.Entry<AtomicLong, AtomicLong> getMaxBucket() {
        Set<Map.Entry<AtomicLong, AtomicLong>> entrySet = counterBuckets.get().entrySet();

        if (entrySet.isEmpty()) {
            return null;
        }

        Comparator<Map.Entry<AtomicLong, AtomicLong>> cmp = new MapEntryValueComparator();

        Map.Entry<AtomicLong, AtomicLong> max = Collections.max(entrySet, cmp);

        return max;
    }

    // remove all but the new current and max buckets
    // if the current and max are same will only leave one bucket in the cache
    void trimBuckets(AtomicLong currentBucketKey) {

        Map.Entry<AtomicLong, AtomicLong> maxBucket = getMaxBucket();
        AtomicLong maxBucketKey = maxBucket.getKey();
        Set<AtomicLong> keySet = counterBuckets.get().keySet();

        for (AtomicLong key : keySet) {
            if ((!key.equals(maxBucketKey)) && (!key.equals(currentBucketKey))) {
                counterBuckets.get().remove(key);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment(long amount) {
        long now = System.currentTimeMillis();
        long ellapsedTime = now - intervalTimestamp.get();
        AtomicLong currentBucketKey = new AtomicLong(TimeUnit.SECONDS.convert(ellapsedTime, TimeUnit.MILLISECONDS));
        AtomicLong currentCount = counterBuckets.get().get(currentBucketKey);

        if (currentCount != null) {
            currentCount.addAndGet(amount);
        } else {
            currentCount = new AtomicLong(amount);
            counterBuckets.get().put(currentBucketKey, currentCount);

            trimBuckets(currentBucketKey);
        }


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