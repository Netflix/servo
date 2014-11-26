/*
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.servo.util;

import com.netflix.servo.jsr166e.ConcurrentHashMapV8;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A semi-persistent mapping from keys to values. Values are automatically loaded
 * by the cache, and are stored in the cache until evicted.
 *
 * @param <K> The type of keys maintained
 * @param <V> The type of values maintained
 */
public class ExpiringCache<K, V> {
    private final ConcurrentHashMapV8<K, Entry<V>> map;
    private final long expireAfterMs;
    private final ConcurrentHashMapV8.Fun<K, Entry<V>> entryGetter;
    private final Clock clock;

    private static final class Entry<V> {
        private volatile long accessTime;
        private final V value;
        private final Clock clock;

        private Entry(V value, long accessTime, Clock clock) {
            this.value = value;
            this.accessTime = accessTime;
            this.clock = clock;
        }

        private V getValue() {
            accessTime = clock.now();
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Entry entry = (Entry) o;

            return accessTime == entry.accessTime && value.equals(entry.value);
        }

        @Override
        public int hashCode() {
            int result = (int) (accessTime ^ (accessTime >>> 32));
            result = 31 * result + value.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Entry{accessTime=" + accessTime + ", value=" + value + '}';
        }
    }

    private static final ScheduledExecutorService SERVICE =
            Executors.newSingleThreadScheduledExecutor(ThreadFactories.withName("expiringMap-%d"));

    /**
     * Create a new ExpiringCache that will expire entries after a given number of milliseconds
     * computing the values as needed using the given getter.
     *
     * @param expireAfterMs Number of milliseconds after which entries will be evicted
     * @param getter        Function that will be used to compute the values
     */
    public ExpiringCache(final long expireAfterMs, final ConcurrentHashMapV8.Fun<K, V> getter) {
        this(expireAfterMs, getter, TimeUnit.MINUTES.toMillis(1), ClockWithOffset.INSTANCE);
    }

    /**
     * For unit tests.
     * Create a new ExpiringCache that will expire entries after a given number of milliseconds
     * computing the values as needed using the given getter.
     *
     * @param expireAfterMs    Number of milliseconds after which entries will be evicted
     * @param getter           Function that will be used to compute the values
     * @param expirationFreqMs Frequency at which to schedule the job that evicts entries
     *                         from the cache.
     */
    public ExpiringCache(final long expireAfterMs, final ConcurrentHashMapV8.Fun<K, V> getter,
                         final long expirationFreqMs, final Clock clock) {
        Preconditions.checkArgument(expireAfterMs > 0, "expireAfterMs must be positive.");
        Preconditions.checkArgument(expirationFreqMs > 0, "expirationFreqMs must be positive.");
        this.map = new ConcurrentHashMapV8<K, Entry<V>>();
        this.expireAfterMs = expireAfterMs;
        this.entryGetter = toEntry(getter);
        this.clock = clock;
        final Runnable expirationJob = new Runnable() {
            @Override
            public void run() {
                long tooOld = clock.now() - expireAfterMs;
                for (Map.Entry<K, Entry<V>> entry : map.entrySet()) {
                    if (entry.getValue().accessTime < tooOld) {
                        map.remove(entry.getKey(), entry.getValue());
                    }
                }
            }
        };
        SERVICE.scheduleWithFixedDelay(expirationJob, 1, expirationFreqMs, TimeUnit.MILLISECONDS);
    }

    private ConcurrentHashMapV8.Fun<K, Entry<V>> toEntry(final ConcurrentHashMapV8.Fun<K, V>
                                                                 underlying) {
        return new ConcurrentHashMapV8.Fun<K, Entry<V>>() {
            @Override
            public Entry<V> apply(K key) {
                return new Entry<V>(underlying.apply(key), 0L, clock);
            }
        };
    }

    /**
     * Get the (possibly cached) value for a given key.
     */
    public V get(final K key) {
        Entry<V> entry = map.computeIfAbsent(key, entryGetter);
        return entry.getValue();
    }

    /**
     * Get the list of all values that are members of this cache. Does not
     * affect the access time used for eviction.
     */
    public List<V> values() {
        final Collection<Entry<V>> values = map.values();
        final List<V> res = new ArrayList<V>(values.size());
        for (Entry<V> e : values) {
            res.add(e.value); // avoid updating the access time
        }
        return Collections.unmodifiableList(res);
    }

    /**
     * Return the number of entries in the cache.
     */
    public int size() {
        return map.size();
    }

    /**{@inheritDoc}*/
    @Override
    public String toString() {
        return "ExpiringCache{"
                + "map=" + map
                + ", expireAfterMs=" + expireAfterMs
                + '}';
    }
}
