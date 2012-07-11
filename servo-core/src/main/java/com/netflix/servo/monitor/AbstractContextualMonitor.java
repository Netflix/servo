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
public abstract class AbstractContextualMonitor<T,M extends Monitor<T>>
        implements CompositeMonitor<T> {

    protected final MonitorConfig baseConfig;
    protected final TaggingContext context;
    protected final Function<MonitorConfig,M> newMonitor;

    protected final ConcurrentMap<MonitorConfig,M> monitors;

    AbstractContextualMonitor(
            MonitorConfig baseConfig,
            TaggingContext context,
            Function<MonitorConfig,M> newMonitor) {
        this.baseConfig = baseConfig;
        this.context = context;
        this.newMonitor = newMonitor;

        monitors = new ConcurrentHashMap<MonitorConfig,M>();
    }

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

    @Override
    public MonitorConfig getConfig() {
        TagList contextTags = context.getTags();
        return MonitorConfig.builder(baseConfig.getName())
            .withTags(baseConfig.getTags())
            .withTags(contextTags)
            .build();
    }

    @Override
    public List<Monitor<?>> getMonitors() {
        return ImmutableList.<Monitor<?>>copyOf(monitors.values());
    }
}
