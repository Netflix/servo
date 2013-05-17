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

    private final AtomicReference<TimestampedHashMap> buckets;
    
    

    public PeakRateCounter(MonitorConfig config) {
        // This class will reset the value so it is not a monotonically increasing value as
        // expected for type=COUNTER. This class looks like a counter to the user and a gauge to
        // the publishing pipeline receiving the value.
        super(config.withAdditionalTag(DataSourceType.GAUGE));
        buckets = new AtomicReference<TimestampedHashMap>(new TimestampedHashMap());
    }

    static class TimestampedHashMap extends ConcurrentHashMap<Long, AtomicLong> {

        private final long timestamp;
    
        /**
         * ConcurrentHashMap is initialized with the goal of reducing memory 
         * and GC load for very large number of counters typically used.
         * 
         * The initialCapacity is set for a small number of values 
         * before reallocation.  
         * The load factor is set high for dense packing
         * The concurrencyLevel is set low for concurrent writes to support
         * a sufficient throughput while reducing unnecessary memory loading
         * 
         */
        TimestampedHashMap() {
            super(8, 0.9f, 1); 
            timestamp = System.currentTimeMillis();
        }

        long getTimestamp() {
            return timestamp;
        }
    }

    @Override
    public Long getValue() {
        return getMaxValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getAndResetValue() {
        Long value = getValue();
        buckets.set(new TimestampedHashMap());
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

        long now = System.currentTimeMillis();
        long ellapsedTime = now - buckets.get().getTimestamp();

        long currentBucketKey = TimeUnit.SECONDS.convert(ellapsedTime, TimeUnit.MILLISECONDS);

        incrementBucket(currentBucketKey, amount);
        trimBuckets(currentBucketKey);
    }

    void incrementBucket(Long bucketKey, long amount) {
    
        AtomicLong count = buckets.get().get(bucketKey);
        
        if (count != null) {
            count.addAndGet(amount);
        } else {
            AtomicLong delta = new AtomicLong(amount);
            count = buckets.get().putIfAbsent(bucketKey, delta);
            if (count != null) {
                count.addAndGet(amount);
            }
        }
    }

    /**
     * Remove all but the current and max buckets.
     */
    void trimBuckets(Long currentBucketKey) {

        Long maxBucketKey = getMaxBucketKey();
       
        Set<Long> keySet = buckets.get().keySet();
        for (Long key : keySet) {
            if ((!key.equals(maxBucketKey)) && (!key.equals(currentBucketKey))) {
                buckets.get().remove(key);
            } 
        }
    }


    static class MapEntryValueComparator implements Comparator<Map.Entry<Long, AtomicLong>>, Serializable {

        @Override
        public int compare(Map.Entry<Long, AtomicLong> o1, Map.Entry<Long, AtomicLong> o2) {
            long v1 = o1.getValue().get();
            long v2 = o2.getValue().get();
            return (v1 == v2 ? 0 : v1 > v2 ? 1 : -1);
        }
    }

    private Map.Entry<Long, AtomicLong> getMaxBucket() {

        Set<Map.Entry<Long, AtomicLong>> entrySet = buckets.get().entrySet();
        if (entrySet.isEmpty()) {
            return null;
        }

        Comparator<Map.Entry<Long, AtomicLong>> cmp = new MapEntryValueComparator();

        Map.Entry<Long, AtomicLong> max = Collections.max(entrySet, cmp);

        return max;
    }
    Long getMaxBucketKey() {
        return getMaxBucket().getKey();
    }
    long getMaxValue() {
        Map.Entry<Long, AtomicLong> bucket = getMaxBucket();  
        return (bucket == null? 0 : getMaxBucket().getValue().get());
    }
    AtomicLong getBucketValue(Long key) {
        return buckets.get().get(key);
    }
}