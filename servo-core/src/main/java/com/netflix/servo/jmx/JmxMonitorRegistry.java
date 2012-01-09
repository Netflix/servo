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

import java.lang.management.ManagementFactory;

import javax.management.DynamicMBean;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.netflix.servo.MonitorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitor registry backed by JMX. The monitor annotations on registered
 * objects will be used to export the data to JMX.
 */
public class JmxMonitorRegistry implements MonitorRegistry {

    private final Logger mLogger = LoggerFactory.getLogger(getClass());

    private final MBeanServer mBeanServer;

    public JmxMonitorRegistry() {
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    private void register(ObjectName name, DynamicMBean mbean)
            throws Exception {
        if (mBeanServer.isRegistered(name)) {
            mBeanServer.unregisterMBean(name);
        }
        mBeanServer.registerMBean(mbean, name);
    }

    public void registerObject(Object obj) {
        Preconditions.checkNotNull("obj cannot be null", obj);
        try {
            MonitoredResource resource = new MonitoredResource(obj);
            register(resource.getObjectName(), resource);

            MetadataMBean metadata = resource.getMetadataMBean();
            register(metadata.getObjectName(), metadata);
        } catch (Throwable t) {
            mLogger.warn("could not register object of class " +
                obj.getClass().getCanonicalName(), t);
        }
    }

    public void unRegisterObject(Object obj) {
        Preconditions.checkNotNull("obj cannot be null", obj);
        try {
            MonitoredResource resource = new MonitoredResource(obj);
            mBeanServer.unregisterMBean(resource.getObjectName());

            MetadataMBean metadata = resource.getMetadataMBean();
            mBeanServer.unregisterMBean(metadata.getObjectName());
        } catch (Throwable t) {
            mLogger.warn("could not un-register object of class " +
                obj.getClass().getCanonicalName(), t);
        }
    }
}
