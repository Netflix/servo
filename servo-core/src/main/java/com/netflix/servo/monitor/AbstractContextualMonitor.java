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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

import com.netflix.servo.tag.TaggingContext;
import com.netflix.servo.tag.TagList;

import java.util.List;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class used to simplify creation of contextual monitors.
 */
public abstract class AbstractContextualMonitor<T, M extends Monitor<T>>
        implements CompositeMonitor<T> {

    /** Base configuration shared across all contexts. */
    protected final MonitorConfig baseConfig;

    /** Context to query when accessing a monitor. */
    protected final TaggingContext context;

    /** Factory funtion used to create a new instance of a monitor. */
    protected final Function<MonitorConfig, M> newMonitor;

    /** Thread-safe map keeping track of the distinct monitors that have been created so far. */
    protected final ConcurrentMap<MonitorConfig, M> monitors;

    /**
     * Create a new instance of the monitor.
     *
     * @param baseConfig  shared configuration
     * @param context     provider for context specific tags
     * @param newMonitor  function to create new monitors
     */
    protected AbstractContextualMonitor(
            MonitorConfig baseConfig,
            TaggingContext context,
            Function<MonitorConfig, M> newMonitor) {
        this.baseConfig = baseConfig;
        this.context = context;
        this.newMonitor = newMonitor;

        monitors = new ConcurrentHashMap<MonitorConfig, M>();
    }

    /**
     * Returns a monitor instance for the current context. If no monitor exists for the current
     * context then a new one will be created.
     */
    protected M getMonitorForCurrentContext() {
        MonitorConfig contextConfig = getConfig();
        M monitor = monitors.get(contextConfig);
        if (monitor == null) {
            M newMon = newMonitor.apply(contextConfig);
            monitor = monitors.putIfAbsent(contextConfig, newMon);
            if (monitor == null) {
                monitor = newMon;
            }
        }
        return monitor;
    }

    /** {@inheritDoc} */
    @Override
    public MonitorConfig getConfig() {
        TagList contextTags = context.getTags();
        return MonitorConfig.builder(baseConfig.getName())
            .withTags(baseConfig.getTags())
            .withTags(contextTags)
            .build();
    }

    /** {@inheritDoc} */
    @Override
    public List<Monitor<?>> getMonitors() {
        return ImmutableList.<Monitor<?>>copyOf(monitors.values());
    }
}
