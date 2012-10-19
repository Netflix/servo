/**
 * Copyright 2012 Netflix, Inc.
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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class that dynamically creates counters based on an arbitrary (name, tagList), or {@link MonitorConfig}
 * Counters are automatically expired after 15 minutes of inactivity.
 */
public class DynamicCounter implements CompositeMonitor<Long> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicCounter.class);
    private static final long DEFAULT_EXPIRATION = 15L;
    private static final TimeUnit DEFAULT_EXPIRATION_UNIT = TimeUnit.MINUTES;
    private static final MonitorConfig baseConfig = new MonitorConfig.Builder("servo").build();

    private static DynamicCounter INSTANCE = new DynamicCounter();

    private DynamicCounter() {
        DefaultMonitorRegistry.getInstance().register(this);
    }

    private final LoadingCache<MonitorConfig, Counter> counters = CacheBuilder.newBuilder()
            .expireAfterAccess(DEFAULT_EXPIRATION, DEFAULT_EXPIRATION_UNIT)
            .build(new CacheLoader<MonitorConfig, Counter>() {
                @Override
                public Counter load(final MonitorConfig config) throws Exception {
                    return new BasicCounter(config);
                }
            });
    private final AtomicLong totalCount = new AtomicLong(0L);

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
        INSTANCE.totalCount.incrementAndGet();
    }

    /**
     * Increment a counter based on a given {@link MonitorConfig} by a given delta.
     * @param config The monitoring config
     * @param delta The amount added to the current value
     */
    public static void increment(MonitorConfig config, long delta) {
        INSTANCE.get(config).increment(delta);
        INSTANCE.totalCount.addAndGet(delta);
    }

    /**
     * Increment the counter for a given name, tagList
     */
    public static void increment(String name, TagList list) {
        MonitorConfig config = new MonitorConfig.Builder(name).withTags(list).build();
        increment(config);
    }

    /**
     * Increment the counter for a given name, tagList by a given delta.
     */
    public static void increment(String name, TagList list, long delta) {
        MonitorConfig config = new MonitorConfig.Builder(name).withTags(list).build();
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
        return totalCount.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MonitorConfig getConfig() {
        return baseConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("baseConfig", baseConfig)
                .add("totalCount", totalCount.get())
                .add("counters", counters.asMap())
                .toString();
    }
}
