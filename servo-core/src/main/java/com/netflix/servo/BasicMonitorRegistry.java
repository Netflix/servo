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
package com.netflix.servo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.netflix.servo.monitor.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple monitor registry backed by a {@link java.util.Set}.
 */
public final class BasicMonitorRegistry implements MonitorRegistry {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Set<Monitor<?>> monitors;

    /**
     * Creates a new instance.
     */
    public BasicMonitorRegistry() {
        monitors = Collections.synchronizedSet(new HashSet<Monitor<?>>());
    }

    /**
     * The set of registered Monitor objects.
     */
    @Override
    public Collection<Monitor<?>> getRegisteredMonitors() {
        return ImmutableList.copyOf(monitors);
    }

    /**
     * Register a new monitor in the registry.
     */
    @Override
    public void register(Monitor<?> monitor) {
        Preconditions.checkNotNull(monitor, "monitor cannot be null");
        try {
            monitors.add(monitor);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid object", e);
        }
    }

    /**
     * Unregister a Monitor from the registry.
     */
    @Override
    public void unregister(Monitor<?> monitor) {
        Preconditions.checkNotNull(monitor, "monitor cannot be null");
        try {
            monitors.remove(monitor);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid object", e);
        }
    }
}
