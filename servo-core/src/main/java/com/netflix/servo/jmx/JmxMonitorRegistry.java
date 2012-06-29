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
package com.netflix.servo.jmx;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.annotations.AnnotatedObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Monitor registry backed by JMX. The monitor annotations on registered
 * objects will be used to export the data to JMX. For details about the
 * representation in JMX see {@link MonitoredResource}.
 */
public final class JmxMonitorRegistry implements MonitorRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(JmxMonitorRegistry.class);

    private final MBeanServer mBeanServer;
    private final Set<Monitor<?>> monitors;
    private final String name;

    /**
     * Creates a new instance that registers metrics with the local mbean
     * server.
     */
    public JmxMonitorRegistry(String name) {
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
        monitors = Collections.synchronizedSet(new HashSet<Monitor<?>>());
        this.name = name;
    }

    private void register(ObjectName name, DynamicMBean mbean)
            throws Exception {
        if (mBeanServer.isRegistered(name)) {
            mBeanServer.unregisterMBean(name);
        }
        mBeanServer.registerMBean(mbean, name);
    }

    /**
     * The set of registered Monitor objects.
     */
    @Override
    public Set<Monitor<?>> getRegisteredMonitors() {
        return ImmutableSet.copyOf(monitors);
    }

    /**
     * Register a new monitor in the registry.
     */
    @Override
    public void register(Monitor<?> monitor) {

        MonitorModelMBean bean = MonitorModelMBean.newInstance(name, monitor);
        try {
            mBeanServer.registerMBean(bean.getMBean(), bean.getObjectName());
            monitors.add(monitor);
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
            mBeanServer.unregisterMBean(MonitorModelMBean.createObjectName(name, monitor.getConfig()));
            monitors.remove(monitor);
        } catch (Exception e) {
            LOG.warn("Unable to un-register Monitor:" + monitor.getConfig(), e);
        }
    }
}
