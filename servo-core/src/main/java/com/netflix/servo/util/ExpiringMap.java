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
package com.netflix.servo.util;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.servo.jsr166e.ConcurrentHashMapV8;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ExpiringMap<K, V> {
    private final ConcurrentHashMapV8<K, Entry<V>> map;
    private final long expireAfterMs;
    private final ConcurrentHashMapV8.Fun<K, Entry<V>> entryGetter;

    private static class Entry<V> {
        private long accessTime;
        private final V value;

        private Entry(V value, long accessTime) {
            this.value = value;
            this.accessTime = accessTime;
        }

        private synchronized V getValue() {
            accessTime = System.currentTimeMillis();
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

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
            return "Entry{" +
                    "accessTime=" + accessTime +
                    ", value=" + value +
                    '}';
        }
    }

    private static final ScheduledExecutorService service;

    static {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("expiringMap-%d")
                .build();
        service = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    public ExpiringMap(final long expireAfterMs, final ConcurrentHashMapV8.Fun<K, V> getter) {
        this(expireAfterMs, getter, TimeUnit.MINUTES.toMillis(1));
    }

    public ExpiringMap(final long expireAfterMs, final ConcurrentHashMapV8.Fun<K, V> getter,
                       final long expirationFreqMs) {
        this.map = new ConcurrentHashMapV8<K, Entry<V>>();
        this.expireAfterMs = expireAfterMs;
        this.entryGetter = toEntry(getter);
        final Runnable expirationJob = new Runnable() {
            @Override
            public void run() {
                long tooOld = System.currentTimeMillis() - expireAfterMs;
                for (Map.Entry<K, Entry<V>> entry : map.entrySet()) {
                    if (entry.getValue().accessTime < tooOld) {
                        map.remove(entry.getKey(), entry.getValue());
                    }
                }
            }
        };
        service.scheduleWithFixedDelay(expirationJob, 1, expirationFreqMs, TimeUnit.MILLISECONDS);
    }

    private ConcurrentHashMapV8.Fun<K, Entry<V>> toEntry(final ConcurrentHashMapV8.Fun<K, V> underlying) {
        return new ConcurrentHashMapV8.Fun<K, Entry<V>>() {
            @Override
            public Entry<V> apply(K key) {
                return new Entry<V>(underlying.apply(key), 0L);
            }
        };
    }

    public V get(final K key) {
        Entry<V> entry = map.computeIfAbsent(key, entryGetter);
        return entry.getValue();
    }

    public List<V> values() {
        ImmutableList.Builder<V> builder = ImmutableList.builder();
        for (Entry<V> e : map.values()) {
            builder.add(e.value); // avoid updating the access time
        }
        return builder.build();
    }

    public int size() {
        return map.size();
    }

    @Override
    public String toString() {
        return "ExpiringMap{"
                + "map=" + map
                + ", expireAfterMs=" + expireAfterMs
                + '}';
    }
}
