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

import com.netflix.servo.jmx.JmxMonitorRegistry;
import com.netflix.servo.monitor.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Collection;

/**
 * Default registry that delegates all actions to a class specified by the
 * {@code com.netflix.servo.DefaultMonitorRegistry.registryClass} property. The
 * specified registry class must have a constructor with no arguments. If the
 * property is not specified or the class cannot be loaded an instance of
 * {@link com.netflix.servo.jmx.JmxMonitorRegistry} will be used.
 */
public final class DefaultMonitorRegistry implements MonitorRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultMonitorRegistry.class);
    private static final String CLASS_NAME = DefaultMonitorRegistry.class.getCanonicalName();
    private static final String REGISTRY_CLASS_PROP = CLASS_NAME + ".registryClass";
    private static final MonitorRegistry INSTANCE = new DefaultMonitorRegistry();
    private static final String DEFAULT_REGISTRY_NAME = "com.netflix.servo";

    private final MonitorRegistry registry;

    /**
     * Returns the instance of this registry.
     */
    public static MonitorRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new instance based on system properties.
     */
    DefaultMonitorRegistry() {
        this(System.getProperties());
    }

    /**
     * Creates a new instance based on the provide properties object. Only
     * intended for use in unit tests.
     */
    DefaultMonitorRegistry(Properties props) {
        String className = props.getProperty(REGISTRY_CLASS_PROP);
        if (className != null) {
            MonitorRegistry r;
            try {
                Class<?> c = Class.forName(className);
                r = (MonitorRegistry) c.newInstance();
            } catch (Throwable t) {
                LOG.error(
                        "failed to create instance of class " + className + ", "
                                + "using default class "
                                + JmxMonitorRegistry.class.getName(),
                        t);
                r = new JmxMonitorRegistry(DEFAULT_REGISTRY_NAME);
            }
            registry = r;
        } else {
            registry = new JmxMonitorRegistry(DEFAULT_REGISTRY_NAME);
        }
    }

    /**
     * The set of registered Monitor objects.
     */
    @Override
    public Collection<Monitor<?>> getRegisteredMonitors() {
        return registry.getRegisteredMonitors();
    }

    /**
     * Register a new monitor in the registry.
     */
    @Override
    public void register(Monitor<?> monitor) {
        registry.register(monitor);
    }

    /**
     * Unregister a Monitor from the registry.
     */
    @Override
    public void unregister(Monitor<?> monitor) {
        registry.unregister(monitor);
    }

    /**
     * Returns the inner registry that was created to service the requests.
     */
    MonitorRegistry getInnerRegistry() {
        return registry;
    }
}
