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
package com.netflix.servo.jmx;

import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.util.UnmodifiableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Monitor registry backed by JMX. The monitor annotations on registered
 * objects will be used to export the data to JMX. For details about the
 * representation in JMX see {@link MonitorMBean}. The {@link ObjectName}
 * used for each monitor depends on the implementation of {@link ObjectNameMapper}
 * that is specified for the registry. See {@link ObjectNameMapper#DEFAULT} for
 * the default naming implementation.
 */
public final class JmxMonitorRegistry implements MonitorRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(JmxMonitorRegistry.class);

  private final MBeanServer mBeanServer;
  private final ConcurrentMap<MonitorConfig, Monitor<?>> monitors;
  private final String name;
  private final ObjectNameMapper mapper;
  private final ConcurrentMap<ObjectName, Object> locks = new ConcurrentHashMap<>();

  private final AtomicBoolean updatePending = new AtomicBoolean(false);
  private final AtomicReference<Collection<Monitor<?>>> monitorList =
      new AtomicReference<>(UnmodifiableList.<Monitor<?>>of());

  /**
   * Creates a new instance that registers metrics with the local mbean
   * server using the default ObjectNameMapper {@link ObjectNameMapper#DEFAULT}.
   *
   * @param name the registry name
   */
  public JmxMonitorRegistry(String name) {
    this(name, ObjectNameMapper.DEFAULT);
  }

  /**
   * Creates a new instance that registers metrics with the local mbean
   * server using the ObjectNameMapper provided.
   *
   * @param name   the registry name
   * @param mapper the monitor to object name mapper
   */
  public JmxMonitorRegistry(String name, ObjectNameMapper mapper) {
    this.name = name;
    this.mapper = mapper;
    mBeanServer = ManagementFactory.getPlatformMBeanServer();
    monitors = new ConcurrentHashMap<>();
  }

  private void register(ObjectName objectName, DynamicMBean mbean) throws Exception {
    synchronized (getLock(objectName)) {
      if (mBeanServer.isRegistered(objectName)) {
        mBeanServer.unregisterMBean(objectName);
      }
      mBeanServer.registerMBean(mbean, objectName);
    }
  }

  /**
   * The set of registered Monitor objects.
   */
  @Override
  public Collection<Monitor<?>> getRegisteredMonitors() {
    if (updatePending.getAndSet(false)) {
      monitorList.set(UnmodifiableList.copyOf(monitors.values()));
    }
    return monitorList.get();
  }

  /**
   * Register a new monitor in the registry.
   */
  @Override
  public void register(Monitor<?> monitor) {
    try {
      List<MonitorMBean> beans = MonitorMBean.createMBeans(name, monitor, mapper);
      for (MonitorMBean bean : beans) {
        register(bean.getObjectName(), bean);
      }
      monitors.put(monitor.getConfig(), monitor);
      updatePending.set(true);
    } catch (Exception e) {
      LOG.warn("Unable to register Monitor:" + monitor.getConfig(), e);
    }
  }

  /**
   * Unregister a Monitor from the registry.
   */
  @Override
  public void unregister(Monitor<?> monitor) {
    try {
      List<MonitorMBean> beans = MonitorMBean.createMBeans(name, monitor, mapper);
      for (MonitorMBean bean : beans) {
        try {
          mBeanServer.unregisterMBean(bean.getObjectName());
          locks.remove(bean.getObjectName());
        } catch (InstanceNotFoundException ignored) {
          // ignore errors attempting to unregister a non-registered monitor
          // a common error is to unregister twice
        }
      }
      monitors.remove(monitor.getConfig());
      updatePending.set(true);
    } catch (Exception e) {
      LOG.warn("Unable to un-register Monitor:" + monitor.getConfig(), e);
    }
  }

  @Override
  public boolean isRegistered(Monitor<?> monitor) {
    try {
      List<MonitorMBean> beans = MonitorMBean.createMBeans(name, monitor, mapper);
      for (MonitorMBean bean : beans) {
        if (mBeanServer.isRegistered(bean.getObjectName())) {
          return true;
        }
      }
    } catch (Exception e) {
      return false;
    }
    return false;
  }

  private Object getLock(ObjectName objectName) {
    return locks.computeIfAbsent(objectName, k -> new Object());
  }
}
