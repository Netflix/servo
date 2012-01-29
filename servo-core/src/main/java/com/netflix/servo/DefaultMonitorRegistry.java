/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 Netflix
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
package com.netflix.servo;

import com.netflix.servo.jmx.JmxMonitorRegistry;

import java.util.Properties;

/**
 * Default registry that delegates all actions to a class specified by the
 * {@code com.netflix.servo.DefaultMonitorRegistry.registryClass} property. The
 * specified registry class must have a constructor with no arguments. If the
 * property is not specified or the class cannot be loaded an instance of
 * {@link com.netflix.servo.jmx.JmxMonitorRegistry} will be used.
 */
public final class DefaultMonitorRegistry implements MonitorRegistry {

    private static final String CLASS_NAME =
        DefaultMonitorRegistry.class.getCanonicalName();

    private static final String REGISTRY_CLASS_PROP =
        CLASS_NAME + ".registryClass";

    private static final MonitorRegistry INSTANCE =
        new DefaultMonitorRegistry();

    private final MonitorRegistry registry;

    /** Returns the instance of this registry. */
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
     * Creates a new instance based on the provide properties object.
     */
    DefaultMonitorRegistry(Properties props) {
        String className = props.getProperty(REGISTRY_CLASS_PROP);
        if (className != null) {
            try {
                Class<?> c = Class.forName(className);
                registry = (MonitorRegistry) c.newInstance();
            } catch (Throwable t) {
                throw new IllegalArgumentException(
                    "failed to create instance of class " + className, t);
            }
        } else {
            registry = new JmxMonitorRegistry();
        }
    }

    /** {@inheritDoc} */
    public void registerObject(Object obj) {
        registry.registerObject(obj);
    }

    /** {@inheritDoc} */
    public void unRegisterObject(Object obj) {
        registry.unRegisterObject(obj);
    }
}
