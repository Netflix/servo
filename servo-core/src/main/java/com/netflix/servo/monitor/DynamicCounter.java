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
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.tag.TagList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Utility class that dynamically creates counters based on an arbitrary (name, tagList), or
 * {@link MonitorConfig}. Counters are automatically expired after 15 minutes of inactivity.
 */
public final class DynamicCounter implements CompositeMonitor<Long> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicCounter.class);
    private static final String DEFAULT_EXPIRATION = "15";
    private static final String DEFAULT_EXPIRATION_UNIT = "MINUTES";
    private static final String DEFAULT_POLLING_INT = "60";
    private static final String DEFAULT_POLLING_INT_UNIT = "SECONDS";
    private static final String CLASS_NAME = DynamicCounter.class.getCanonicalName();
    private static final String EXPIRATION_PROP = CLASS_NAME + ".expiration";
    private static final String EXPIRATION_PROP_UNIT = CLASS_NAME + ".expirationUnit";
    private static final String POLLING_INT_PROP = CLASS_NAME + ".pollingInterval";
    private static final String POLLING_INT_PROP_UNIT = CLASS_NAME + ".pollingIntervalUnit";
    private static final String INTERNAL_ID = "servoCounters";
    private static final String CACHE_MONITOR_ID = "servoCountersCache";
    private static final MonitorConfig BASE_CONFIG = new MonitorConfig.Builder(INTERNAL_ID).build();

    private static final DynamicCounter INSTANCE = new DynamicCounter();

    private final LoadingCache<MonitorConfig, Counter> counters;
    private final CompositeMonitor<?> cacheMonitor;

    private DynamicCounter() {
        final String expiration = System.getProperty(EXPIRATION_PROP, DEFAULT_EXPIRATION);
        final String expirationUnit =
            System.getProperty(EXPIRATION_PROP_UNIT, DEFAULT_EXPIRATION_UNIT);
        final long expirationValue = Long.valueOf(expiration);
        final TimeUnit expirationUnitValue = TimeUnit.valueOf(expirationUnit);

        final String interval = System.getProperty(POLLING_INT_PROP, DEFAULT_POLLING_INT);
        final String intervalUnit =
            System.getProperty(POLLING_INT_PROP_UNIT, DEFAULT_POLLING_INT_UNIT);
        final long pollingInterval = Long.valueOf(interval);
        final TimeUnit pollingUnit = TimeUnit.valueOf(intervalUnit);
        final long pollingIntervalMs = pollingUnit.toMillis(pollingInterval);
        counters = CacheBuilder.newBuilder()
                .expireAfterAccess(expirationValue, expirationUnitValue)
                .build(new CacheLoader<MonitorConfig, Counter>() {
                    @Override
                    public Counter load(final MonitorConfig config) throws Exception {
                        return new ResettableCounter(config, pollingIntervalMs);
                    }
                });
        cacheMonitor = Monitors.newCacheMonitor(CACHE_MONITOR_ID, counters);
        DefaultMonitorRegistry.getInstance().register(this);
    }

    private Counter get(MonitorConfig config) {
        try {
            return counters.get(config);
        } catch (ExecutionException e) {
            LOGGER.error("Failed to get a counter for {}: {}", config, e.getMessage());
            throw Throwables.propagate(e);
        }
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
        for (int i = 0; i < tags.length; i += 2) {
            configBuilder.withTag(tags[i], tags[i + 1]);
        }
        increment(configBuilder.build());
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
        final ConcurrentMap<MonitorConfig, Counter> countersMap = counters.asMap();
        return ImmutableList.<Monitor<?>>copyOf(countersMap.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getValue() {
        return (long) counters.asMap().size();
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
        ConcurrentMap<?, ?> map = counters.asMap();
        return Objects.toStringHelper(this)
                .add("baseConfig", BASE_CONFIG)
                .add("totalCounters", map.size())
                .add("counters", map)
                .toString();
    }
}
