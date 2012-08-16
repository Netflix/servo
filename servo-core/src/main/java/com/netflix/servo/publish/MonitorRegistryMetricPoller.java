/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 - 2012 Netflix
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.netflix.servo.publish;

import com.google.common.collect.Lists;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.Metric;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.CompositeMonitor;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.ResettableMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Poller for fetching {@link com.netflix.servo.annotations.Monitor} metrics
 * from a monitor registry.
 */
public final class MonitorRegistryMetricPoller implements MetricPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorRegistryMetricPoller.class);

    private final MonitorRegistry registry;

    private final long cacheTTL;

    private final AtomicReference<List<Monitor<?>>> cachedMonitors =
        new AtomicReference<List<Monitor<?>>>();

    private final AtomicLong cacheLastUpdateTime = new AtomicLong(0L);

    /**
     * Creates a new instance using {@link com.netflix.servo.DefaultMonitorRegistry}.
     */
    public MonitorRegistryMetricPoller() {
        this(DefaultMonitorRegistry.getInstance(), 0L);
    }

    /**
     * Creates a new instance using the specified registry.
     *
     * @param registry  registry to query for annotated objects
     */
    public MonitorRegistryMetricPoller(MonitorRegistry registry) {
        this(registry, 0L);
    }

    /**
     * Creates a new instance using the specified registry.
     *
     * @param registry  registry to query for annotated objects
     * @param cacheTTL  how long to cache the filtered monitor list from the registry
     */
    public MonitorRegistryMetricPoller(MonitorRegistry registry, long cacheTTL) {
        this.registry = registry;
        this.cacheTTL = cacheTTL;
    }

    private Object getValue(Monitor<?> monitor, boolean reset) {
        try {
            if (reset && monitor instanceof ResettableMonitor<?>) {
                return ((ResettableMonitor<?>) monitor).getAndResetValue();
            } else {
                return monitor.getValue();
            }
        } catch (Exception e) {
            LOGGER.warn("failed to get value for " + monitor.getConfig(), e);
            return null;
        }
    }

    private void getMonitors(List<Monitor<?>> monitors, MetricFilter filter, Monitor<?> monitor) {
        if (monitor instanceof CompositeMonitor<?>) {
            for (Monitor<?> m : ((CompositeMonitor<?>) monitor).getMonitors()) {
                getMonitors(monitors, filter, m);
            }
        } else if (filter.matches(monitor.getConfig())) {
            monitors.add(monitor);
        }
    }

    private void refreshMonitorCache(MetricFilter filter) {
        final long age = System.currentTimeMillis() - cacheLastUpdateTime.get();
        if (age > cacheTTL) {
            List<Monitor<?>> monitors = Lists.newArrayList();
            for (Monitor<?> monitor : registry.getRegisteredMonitors()) {
                try {
                    getMonitors(monitors, filter, monitor);
                } catch (Exception e) {
                    LOGGER.warn("failed to get monitors for composite " + monitor.getConfig(), e);
                }
            }
            cacheLastUpdateTime.set(System.currentTimeMillis());
            cachedMonitors.set(monitors);
        }
    }

    /** {@inheritDoc} */
    public List<Metric> poll(MetricFilter filter) {
        return poll(filter, false);
    }

    /** {@inheritDoc} */
    public List<Metric> poll(MetricFilter filter, boolean reset) {
        refreshMonitorCache(filter);
        List<Monitor<?>> monitors = cachedMonitors.get();
        List<Metric> metrics = Lists.newArrayListWithCapacity(monitors.size());
        for (Monitor<?> monitor : monitors) {
            Object v = getValue(monitor, reset);
            if (v != null) {
                long now = System.currentTimeMillis();
                metrics.add(new Metric(monitor.getConfig(), now, v));
            }
        }
        return metrics;
    }
}
