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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.jsr166e.ConcurrentHashMapV8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Utility class that dynamically creates counters based on an arbitrary (name, tagList), or
 * {@link MonitorConfig}. Counters are automatically expired after 15 minutes of inactivity.
 */
public final class DynamicCounter implements CompositeMonitor<Long> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicCounter.class);
    private static final String DEFAULT_EXPIRATION = "15";
    private static final String DEFAULT_EXPIRATION_UNIT = "MINUTES";
    private static final String CLASS_NAME = DynamicCounter.class.getCanonicalName();
    private static final String EXPIRATION_PROP = CLASS_NAME + ".expiration";
    private static final String EXPIRATION_PROP_UNIT = CLASS_NAME + ".expirationUnit";
    private static final String INTERNAL_ID = "servoCounters";
    private static final MonitorConfig BASE_CONFIG = new MonitorConfig.Builder(INTERNAL_ID).build();

    private static final DynamicCounter INSTANCE = new DynamicCounter();

    private final ConcurrentHashMapV8<MonitorConfig, Entry> counters;
    private final long expireAfterMs;

    private static class Entry {
        private long accessTime;
        private final ResettableCounter counter;

        private Entry(ResettableCounter counter, long accessTime) {
            this.counter = counter;
            this.accessTime = accessTime;
        }

        private synchronized ResettableCounter getCounter() {
            accessTime = System.currentTimeMillis();
            return counter;
        }

        static Entry newEntry(final MonitorConfig config) {
            return new Entry(new ResettableCounter(config), 0L);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            return accessTime == entry.accessTime && counter.equals(entry.counter);
        }

        @Override
        public int hashCode() {
            int result = (int) (accessTime ^ (accessTime >>> 32));
            result = 31 * result + counter.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "accessTime=" + accessTime +
                    ", counter=" + counter +
                    '}';
        }
    }

    final Runnable expirationJob = new Runnable() {
        @Override
        public void run() {
            long tooOld = System.currentTimeMillis() - expireAfterMs;
            for (Map.Entry<MonitorConfig, Entry> entry : counters.entrySet()) {
                if (entry.getValue().accessTime < tooOld) {
                    counters.remove(entry.getKey(), entry.getValue());
                }
            }
        }
    };

    final ScheduledExecutorService service;

    private DynamicCounter() {
        final String expiration = System.getProperty(EXPIRATION_PROP, DEFAULT_EXPIRATION);
        final String expirationUnit =
            System.getProperty(EXPIRATION_PROP_UNIT, DEFAULT_EXPIRATION_UNIT);
        final long expirationValue = Long.valueOf(expiration);
        final TimeUnit expirationUnitValue = TimeUnit.valueOf(expirationUnit);
        expireAfterMs = expirationUnitValue.toMillis(expirationValue);
        counters = new ConcurrentHashMapV8<MonitorConfig, Entry>();
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("dynCounterExpiration-%d")
                .build();
        service = Executors.newSingleThreadScheduledExecutor(threadFactory);
        service.scheduleWithFixedDelay(expirationJob, 1, 1, TimeUnit.MINUTES);
        DefaultMonitorRegistry.getInstance().register(this);
    }

    private Counter get(final MonitorConfig config) {
        Entry entry = counters.computeIfAbsent(config, new ConcurrentHashMapV8.Fun<MonitorConfig, Entry>() {
            @Override
            public Entry apply(final MonitorConfig monitorConfig) {
                return Entry.newEntry(config);
            }
        });
        return entry.getCounter();
    }

    /**
     * Increment a counter based on a given {@link MonitorConfig}.
     */
    public static void increment(MonitorConfig config) {
        INSTANCE.get(config).increment();
    }

    /**
     * Increment a counter specified by a name, and a sequence of (key, value) pairs.
     */
    public static void increment(String name, String... tags) {
        final MonitorConfig.Builder configBuilder = MonitorConfig.builder(name);
        Preconditions.checkArgument(tags.length % 2 == 0,
                "The sequence of (key, value) pairs must have even size: one key, one value");
        try {
            for (int i = 0; i < tags.length; i += 2) {
                configBuilder.withTag(tags[i], tags[i + 1]);
            }
            increment(configBuilder.build());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to get a counter to increment: {}", e.getMessage());
        }
    }


    /**
     * Increment a counter based on a given {@link MonitorConfig} by a given delta.
     * @param config The monitoring config
     * @param delta The amount added to the current value
     */
    public static void increment(MonitorConfig config, long delta) {
        INSTANCE.get(config).increment(delta);
    }

    /**
     * Increment the counter for a given name, tagList.
     */
    public static void increment(String name, TagList list) {
        final MonitorConfig config = new MonitorConfig.Builder(name).withTags(list).build();
        increment(config);
    }

    /**
     * Increment the counter for a given name, tagList by a given delta.
     */
    public static void increment(String name, TagList list, long delta) {
        final MonitorConfig config = MonitorConfig.builder(name).withTags(list).build();
        increment(config, delta);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Monitor<?>> getMonitors() {
        ImmutableList.Builder<Monitor<?>> builder = ImmutableList.builder();
        for (Entry e : counters.values()) {
            builder.add(e.counter); // avoid updating the access time
        }
        return builder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getValue() {
        return (long) counters.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MonitorConfig getConfig() {
        return BASE_CONFIG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        ConcurrentMap<?, ?> map = counters;
        return Objects.toStringHelper(this)
                .add("baseConfig", BASE_CONFIG)
                .add("totalCounters", map.size())
                .add("counters", map)
                .toString();
    }
}
