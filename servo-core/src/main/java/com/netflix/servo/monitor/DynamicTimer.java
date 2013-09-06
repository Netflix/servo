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
 * Utility class that dynamically creates {@link BasicTimer}s based on an arbitrary
 * (name, tagList), or {@link MonitorConfig}. Timers are automatically expired after 15 minutes of
 * inactivity.
 */
public final class DynamicTimer implements CompositeMonitor<Long> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTimer.class);
    private static final String DEFAULT_EXPIRATION = "15";
    private static final String DEFAULT_EXPIRATION_UNIT = "MINUTES";
    private static final String CLASS_NAME = DynamicTimer.class.getCanonicalName();
    private static final String EXPIRATION_PROP = CLASS_NAME + ".expiration";
    private static final String EXPIRATION_PROP_UNIT = CLASS_NAME + ".expirationUnit";
    private static final String INTERNAL_ID = "servoTimers";
    private static final String CACHE_MONITOR_ID = "servoTimersCache";
    private static final MonitorConfig BASE_CONFIG = new MonitorConfig.Builder(INTERNAL_ID).build();

    private static final DynamicTimer INSTANCE = new DynamicTimer();

    private final LoadingCache<ConfigUnit, Timer> timers;
    private final CompositeMonitor<?> cacheMonitor;

    static class ConfigUnit {
        final MonitorConfig config;
        final TimeUnit unit;

        ConfigUnit(MonitorConfig config, TimeUnit unit) {
            this.config = config;
            this.unit = unit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ConfigUnit that = (ConfigUnit) o;
            return config.equals(that.config) && unit == that.unit;
        }

        @Override
        public int hashCode() {
            int result = config.hashCode();
            result = 31 * result + unit.hashCode();
            return result;
        }
    }

    private DynamicTimer() {
        final String expiration = System.getProperty(EXPIRATION_PROP, DEFAULT_EXPIRATION);
        final String expirationUnit =
            System.getProperty(EXPIRATION_PROP_UNIT, DEFAULT_EXPIRATION_UNIT);
        final long expirationValue = Long.valueOf(expiration);
        final TimeUnit expirationUnitValue = TimeUnit.valueOf(expirationUnit);
        timers = CacheBuilder.newBuilder()
                .expireAfterAccess(expirationValue, expirationUnitValue)
                .build(new CacheLoader<ConfigUnit, Timer>() {
                    @Override
                    public Timer load(final ConfigUnit configUnit) throws Exception {
                        return new BasicTimer(configUnit.config, configUnit.unit);
                    }
                });
        cacheMonitor = Monitors.newCacheMonitor(CACHE_MONITOR_ID, timers);
        DefaultMonitorRegistry.getInstance().register(this);
    }

    private Timer get(MonitorConfig config, TimeUnit unit) {
        try {
            return timers.get(new ConfigUnit(config, unit));
        } catch (ExecutionException e) {
            LOGGER.error("Failed to get a timer for {}: {}", config, e.getMessage());
            throw Throwables.propagate(e);
        }
    }

    /**
     * Returns a stopwatch that has been started and will automatically
     * record its result to the dynamic timer specified by the given config. The timer
     * uses the specified TimeUnit.
     */
    public static Stopwatch start(MonitorConfig config, TimeUnit unit) {
        return INSTANCE.get(config, unit).start();
    }

    /**
     * Returns a stopwatch that has been started and will automatically
     * record its result to the dynamic timer specified by the given config. The timer
     * uses a TimeUnit of milliseconds.
     */
    public static Stopwatch start(MonitorConfig config) {
        return INSTANCE.get(config, TimeUnit.MILLISECONDS).start();
    }

    /**
     * Record result to the dynamic timer indicated by the provided config
     * with a TimeUnit of milliseconds.
     */
    public static void record(MonitorConfig config, long duration) {
        INSTANCE.get(config, TimeUnit.MILLISECONDS).record(duration);
    }

    /**
     * Record result to the dynamic timer indicated by the provided config.
     */
    public static void record(MonitorConfig config, long duration, TimeUnit unit) {
        INSTANCE.get(config, unit).record(duration);
    }

    /**
     * Returns a stopwatch that has been started and will automatically
     * record its result to the dynamic timer specified by the given name, and sequence of (key,
     * value) pairs. The timer uses a TimeUnit of milliseconds.
     */
    public static Stopwatch start(String name, String... tags) {
        final MonitorConfig.Builder configBuilder = MonitorConfig.builder(name);
        Preconditions.checkArgument(tags.length % 2 == 0,
                "The sequence of (key, value) pairs must have even size: one key, one value");
        for (int i = 0; i < tags.length; i += 2) {
            configBuilder.withTag(tags[i], tags[i + 1]);
        }

        return INSTANCE.get(configBuilder.build(), TimeUnit.MILLISECONDS).start();
    }

    /**
     * Returns a stopwatch that has been started and will automatically
     * record its result to the dynamic timer specified by the given config. The timer
     * uses a TimeUnit of milliseconds.
     */
    public static Stopwatch start(String name, TagList list) {
        final MonitorConfig config = new MonitorConfig.Builder(name).withTags(list).build();
        return INSTANCE.get(config, TimeUnit.MILLISECONDS).start();
    }

    /**
     * Returns a stopwatch that has been started and will automatically
     * record its result to the dynamic timer specified by the given config. The timer
     * uses a TimeUnit of milliseconds.
     */
    public static Stopwatch start(String name, TagList list, TimeUnit unit) {
        final MonitorConfig config = new MonitorConfig.Builder(name).withTags(list).build();
        return INSTANCE.get(config, unit).start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Monitor<?>> getMonitors() {
        final ConcurrentMap<ConfigUnit, Timer> timersMap = timers.asMap();
        return ImmutableList.<Monitor<?>>copyOf(timersMap.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getValue() {
        return (long) timers.asMap().size();
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
        ConcurrentMap<?, ?> map = timers.asMap();
        return Objects.toStringHelper(this)
                .add("baseConfig", BASE_CONFIG)
                .add("totalTimers", map.size())
                .add("timers", map)
                .toString();
    }
}
