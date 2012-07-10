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
import com.netflix.servo.monitor.CompositeMonitor;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.NumericMonitor;
import com.netflix.servo.monitor.ResettableMonitor;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.tag.SortedTagList;
import com.netflix.servo.tag.TagList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Poller for fetching {@link com.netflix.servo.annotations.Monitor} metrics
 * from a monitor registry.
 */
public final class MonitorRegistryMetricPoller implements MetricPoller {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MonitorRegistryMetricPoller.class);

    private final MonitorRegistry registry;

    /**
     * Creates a new instance using
     * {@link com.netflix.servo.DefaultMonitorRegistry}.
     */
    public MonitorRegistryMetricPoller() {
        this(DefaultMonitorRegistry.getInstance());
    }

    /**
     * Creates a new instance using the specified registry.
     *
     * @param registry  registry to query for annotated objects
     */
    public MonitorRegistryMetricPoller(MonitorRegistry registry) {
        this.registry = registry;
    }

    private Number getValue(Monitor<?> monitor, boolean reset) {
        if (reset && monitor instanceof ResettableMonitor<?>) {
            return (Number) ((ResettableMonitor<?>) monitor).getAndResetValue();
        } else {
            return (Number) monitor.getValue();
        }
    }

    private void getMetrics(
            List<Metric> metrics,
            MetricFilter filter,
            boolean reset,
            Monitor<?> monitor)
            throws Exception {
        LOGGER.debug("get metrics for: " + monitor);
        if (monitor instanceof CompositeMonitor<?>) {
            for (Monitor<?> m : ((CompositeMonitor<?>) monitor).getMonitors()) {
                getMetrics(metrics, filter, reset, m);
            }
        } else if (monitor instanceof NumericMonitor<?> && filter.matches(monitor.getConfig())) {
            Number n = getValue(monitor, reset);
            long now = System.currentTimeMillis();
            metrics.add(new Metric(monitor.getConfig(), now, n));
        }
    }

    /** {@inheritDoc} */
    public List<Metric> poll(MetricFilter filter) {
        return poll(filter, false);
    }

    /** {@inheritDoc} */
    public List<Metric> poll(MetricFilter filter, boolean reset) {
        List<Metric> metrics = Lists.newArrayList();
        for (Monitor<?> monitor : registry.getRegisteredMonitors()) {
            try {
                getMetrics(metrics, filter, reset, monitor);
            } catch (Exception e) {
                LOGGER.warn("failed to get values for {}", e, monitor.getConfig());
            }
        }
        return metrics;
    }
}
