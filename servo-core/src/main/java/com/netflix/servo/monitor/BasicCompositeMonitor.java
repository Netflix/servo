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

import java.util.List;

/**
 * Simple composite monitor type with a static list of sub-monitors. The value for the composite
 * is the number of sub-monitors.
 */
public final class BasicCompositeMonitor implements CompositeMonitor<Integer> {

    private final MonitorConfig config;
    private final List<Monitor<?>> monitors;

    /**
     * Create a new composite.
     *
     * @param config    configuration for the composite. It is recommended that the configuration
     *                  shares common tags with the sub-monitors, but it is not enforced.
     * @param monitors  list of sub-monitors
     */
    public BasicCompositeMonitor(MonitorConfig config, List<Monitor<?>> monitors) {
        this.config = Preconditions.checkNotNull(config);
        this.monitors = ImmutableList.copyOf(monitors);
    }

    /** {@inheritDoc} */
    @Override
    public Integer getValue() {
        return monitors.size();
    }

    /** {@inheritDoc} */
    @Override
    public MonitorConfig getConfig() {
        return config;
    }

    /** {@inheritDoc} */
    @Override
    public List<Monitor<?>> getMonitors() {
        return monitors;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof BasicCompositeMonitor)) {
            return false;
        }
        BasicCompositeMonitor m = (BasicCompositeMonitor) obj;
        return config.equals(m.getConfig()) && monitors.equals(m.getMonitors());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(config, monitors);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("config", config)
                .add("monitors", monitors)
                .toString();
    }
}
