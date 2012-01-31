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
package com.netflix.servo.jmx;

import com.google.common.base.Preconditions;

import com.google.common.collect.ImmutableSet;

import com.netflix.servo.MonitorRegistry;

import java.lang.management.ManagementFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.DynamicMBean;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitor registry backed by JMX. The monitor annotations on registered
 * objects will be used to export the data to JMX. For details about the
 * representation in JMX see {@link MonitoredResource}.
 */
public final class JmxMonitorRegistry implements MonitorRegistry {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MBeanServer mBeanServer;

    private final Set<Object> objects;

    /**
     * Creates a new instance that registers metrics with the local mbean
     * server.
     */
    public JmxMonitorRegistry() {
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
        objects = Collections.synchronizedSet(new HashSet<Object>());
    }

    private void register(ObjectName name, DynamicMBean mbean)
            throws Exception {
        if (mBeanServer.isRegistered(name)) {
            mBeanServer.unregisterMBean(name);
        }
        mBeanServer.registerMBean(mbean, name);
    }

    /** {@inheritDoc} */
    public void registerObject(Object obj) {
        Preconditions.checkNotNull(obj, "obj cannot be null");
        try {
            MonitoredResource resource = new MonitoredResource(obj);
            register(resource.getObjectName(), resource);

            MetadataMBean metadata = resource.getMetadataMBean();
            register(metadata.getObjectName(), metadata);

            objects.add(obj);
        } catch (Throwable t) {
            logger.warn("could not register object of class "
                + obj.getClass().getCanonicalName(), t);
        }
    }

    /** {@inheritDoc} */
    public void unRegisterObject(Object obj) {
        Preconditions.checkNotNull(obj, "obj cannot be null");
        try {
            MonitoredResource resource = new MonitoredResource(obj);
            mBeanServer.unregisterMBean(resource.getObjectName());

            MetadataMBean metadata = resource.getMetadataMBean();
            mBeanServer.unregisterMBean(metadata.getObjectName());

            objects.remove(obj);
        } catch (Throwable t) {
            logger.warn("could not un-register object of class "
                + obj.getClass().getCanonicalName(), t);
        }
    }

    /** {@inheritDoc} */
    public Set<Object> getRegisteredObjects() {
        return ImmutableSet.copyOf(objects);
    }
}
