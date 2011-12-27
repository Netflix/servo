package com.netflix.monitoring;

import com.google.common.base.Preconditions;

import java.lang.management.ManagementFactory;

import javax.management.DynamicMBean;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitor registry backed by JMX. The monitor annotations on registered
 * objects will be used to export the data to JMX.
 */
public class JmxMonitorRegistry implements IMonitorRegistry {

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
