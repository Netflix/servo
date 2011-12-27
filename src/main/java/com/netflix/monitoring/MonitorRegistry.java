package com.netflix.monitoring;

import java.util.Properties;

public class MonitorRegistry implements IMonitorRegistry {

    private static final String REGISTRY_CLASS_PROP =
        "com.netflix.monitoring.registryClass";

    private static IMonitorRegistry INSTANCE = new MonitorRegistry();

    private final IMonitorRegistry mRegistry;

    private static IMonitorRegistry getInstance() {
        return INSTANCE;
    }

    MonitorRegistry() {
        this(System.getProperties());
    }

    MonitorRegistry(Properties props) {
        String className = props.getProperty(REGISTRY_CLASS_PROP);
        if (className != null) {
            try {
                Class<?> c = Class.forName(className);
                mRegistry = (IMonitorRegistry) c.newInstance();
            } catch (Throwable t) {
                throw new IllegalArgumentException(
                    "failed to create instance of class " + className, t);
            }
        } else {
            mRegistry = new JmxMonitorRegistry();
        }
    }

    public void registerObject(Object obj) {
        mRegistry.registerObject(obj);
    }

    public void unRegisterObject(Object obj) {
        mRegistry.unRegisterObject(obj);
    }
}
