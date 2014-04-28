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
import com.google.common.collect.ImmutableList;

import com.netflix.servo.tag.TagList;

import java.util.List;

/**
 * Wraps another composite monitor object providing an alternative configuration.
 */
class CompositeMonitorWrapper<T> extends AbstractMonitor<T> implements CompositeMonitor<T> {

    private final TagList tags;
    private final CompositeMonitor<T> monitor;

    /** Creates a new instance of the wrapper. */
    public CompositeMonitorWrapper(TagList tags, CompositeMonitor<T> monitor) {
        super(monitor.getConfig().withAdditionalTags(tags));
        this.tags = tags;
        this.monitor = monitor;
    }

    /** {@inheritDoc} */
    @Override
    public List<Monitor<?>> getMonitors() {
        List<Monitor<?>> monitors = monitor.getMonitors();
        ImmutableList.Builder<Monitor<?>> builder = ImmutableList.builder();
        for (Monitor<?> m : monitors) {
            builder.add(Monitors.wrap(tags, m));
        }
        return builder.build();
    }

    /** {@inheritDoc} */
    @Override
    public T getValue() {
        return monitor.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CompositeMonitorWrapper<?>)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        CompositeMonitorWrapper<T> m = (CompositeMonitorWrapper<T>) obj;
        return config.equals(m.getConfig()) && monitor.equals(m.monitor);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(config, monitor);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("config", config)
                .add("monitor", monitor)
                .toString();
    }
}
