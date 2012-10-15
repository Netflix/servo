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
import com.netflix.servo.tag.TagList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class CompositeCounter implements CompositeMonitor<Long> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeCounter.class);
    private static final long DEFAULT_EXPIRATION = 15L;
    private static final TimeUnit DEFAULT_EXPIRATION_UNIT = TimeUnit.MINUTES;

    private final MonitorConfig baseConfig;
    private final LoadingCache<TagList, Counter> counters;
    private final AtomicLong totalCount = new AtomicLong(0L);

    /**
     * A composite counter that allows increment from arbitrary tagLists. 
     * Counters will no longer be reported after a 15 minute period of inactivity.
     */
    public CompositeCounter(MonitorConfig baseConfig) {
        this(baseConfig, DEFAULT_EXPIRATION, DEFAULT_EXPIRATION_UNIT);
    }


    /**
     * A composite counter that allows increment from arbitrary tagLists. 
     * Counters will no longer be reported after the specified period of inactivity.
     */
    public CompositeCounter(MonitorConfig config, long expiration, TimeUnit unit) {
        this.baseConfig = config;
        counters = CacheBuilder.newBuilder().expireAfterAccess(expiration, unit).build(new CacheLoader<TagList, Counter>() {
            @Override
            public Counter load(final TagList list) throws Exception {
                return new BasicCounter(baseConfig.withAdditionalTags(list));
            }
        });
    }

    private Counter getCounter(final TagList list) {
        try {
            return counters.get(list);
        } catch (ExecutionException e) {
            LOGGER.error("Failed to get a counter for {}: {}", list, e.getMessage());
            throw Throwables.propagate(e);
        }
    }

    /**
     * Increment the counter for a given TagList.
     */
    public void increment(TagList list) {
        getCounter(list).increment();
        totalCount.incrementAndGet();
    }

    /**
     * Increment the counter for a given TagList by a given value.
     */
    public void increment(TagList list, long byValue) {
        getCounter(list).increment(byValue);
        totalCount.addAndGet(byValue);
    }


    /** {@inheritDoc} */
    @Override
    public List<Monitor<?>> getMonitors() {
        final ConcurrentMap<TagList, Counter> countersMap = counters.asMap();
        return ImmutableList.<Monitor<?>>copyOf(countersMap.values());
    }

    /** {@inheritDoc} */
    @Override
    public Long getValue() {
        return totalCount.get();
    }

    /** {@inheritDoc} */
    @Override
    public MonitorConfig getConfig() {
        return baseConfig;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompositeCounter that = (CompositeCounter) o;

        return baseConfig.equals(that.baseConfig)
                && totalCount.get() == that.totalCount.get()
                && counters.equals(that.counters);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(baseConfig, totalCount.get(), counters);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("baseConfig", baseConfig)
                .add("totalCount", totalCount.get())
                .add("counters", counters)
                .toString();
    }
}
