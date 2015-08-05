/**
 * Copyright 2013 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo;

import com.netflix.servo.jmx.JmxMonitorRegistry;
import com.netflix.servo.jmx.ObjectNameMapper;
import com.netflix.servo.monitor.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Properties;

/**
 * Default registry that delegates all actions to a class specified by the
 * {@code com.netflix.servo.DefaultMonitorRegistry.registryClass} property. The
 * specified registry class must have a constructor with no arguments. If the
 * property is not specified or the class cannot be loaded an instance of
 * {@link com.netflix.servo.jmx.JmxMonitorRegistry} will be used.
 * <p/>
 * If the default {@link com.netflix.servo.jmx.JmxMonitorRegistry} is used, the property
 * {@code com.netflix.servo.DefaultMonitorRegistry.jmxMapperClass} can optionally be
 * specified to control how monitors are mapped to JMX {@link javax.management.ObjectName}.
 * This property specifies the {@link com.netflix.servo.jmx.ObjectNameMapper}
 * implementation class to use. The implementation must have a constructor with
 * no arguments.
 */
public final class DefaultMonitorRegistry implements MonitorRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultMonitorRegistry.class);
  private static final String CLASS_NAME = DefaultMonitorRegistry.class.getCanonicalName();
  private static final String REGISTRY_CLASS_PROP = CLASS_NAME + ".registryClass";
  private static final String REGISTRY_NAME_PROP = CLASS_NAME + ".registryName";
  private static final String REGISTRY_JMX_NAME_PROP = CLASS_NAME + ".jmxMapperClass";
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
    final String className = props.getProperty(REGISTRY_CLASS_PROP);
    final String registryName = props.getProperty(REGISTRY_NAME_PROP, DEFAULT_REGISTRY_NAME);
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
        r = new JmxMonitorRegistry(registryName);
      }
      registry = r;
    } else {
      registry = new JmxMonitorRegistry(registryName,
          getObjectNameMapper(props));
    }
  }

  /**
   * Gets the {@link ObjectNameMapper} to use by looking at the
   * {@code com.netflix.servo.DefaultMonitorRegistry.jmxMapperClass}
   * property. If not specified, then {@link ObjectNameMapper#DEFAULT}
   * is used.
   *
   * @param props the properties
   * @return the mapper to use
   */
  private static ObjectNameMapper getObjectNameMapper(Properties props) {
    ObjectNameMapper mapper = ObjectNameMapper.DEFAULT;
    final String jmxNameMapperClass = props.getProperty(REGISTRY_JMX_NAME_PROP);
    if (jmxNameMapperClass != null) {
      try {
        Class<?> mapperClazz = Class.forName(jmxNameMapperClass);
        mapper = (ObjectNameMapper) mapperClazz.newInstance();
      } catch (Throwable t) {
        LOG.error(
            "failed to create the JMX ObjectNameMapper instance of class "
                + jmxNameMapperClass
                + ", using the default naming scheme",
            t);
      }
    }

    return mapper;
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

  @Override
  public boolean isRegistered(Monitor<?> monitor) {
    return registry.isRegistered(monitor);
  }
}
