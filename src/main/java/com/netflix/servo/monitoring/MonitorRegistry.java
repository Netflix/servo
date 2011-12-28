/*
 * Copyright (c) 2011. Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package com.netflix.servo.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thin wrapper to the platform {@link javax.management.MBeanServer MBeanServer}
 *
 * @author gkim
 */
public class MonitorRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorRegistry.class);


    public enum Namespace {PLATFORM, APPLICATION}

    ;

    public final static String DOMAIN_NAME = "com.netflix.MonitoredResources";

    private static MonitorRegistry s_instance = new MonitorRegistry();

    private final MBeanServer _mbeanServer;

    private final Map<Object, ObjectName> _platformMBeansMap;

    private final Map<Object, ObjectName> _appMBeansMap;

    private MonitorRegistry() {
        _mbeanServer = ManagementFactory.getPlatformMBeanServer();
        _platformMBeansMap = new ConcurrentHashMap<Object, ObjectName>();
        _appMBeansMap = new ConcurrentHashMap<Object, ObjectName>();
    }

    public static MonitorRegistry getInstance() {
        return s_instance;
    }

    /**
     * Return the currently registered MBean {@link ObjectName}s in the provided
     * {@link Namespace}
     */
    public ObjectName[] getRegisteredObjectNames(Namespace namespace) {
        if (namespace == Namespace.PLATFORM) {
            return _platformMBeansMap.values().
                    toArray(new ObjectName[_platformMBeansMap.size()]);
        } else {
            return _appMBeansMap.values().
                    toArray(new ObjectName[_appMBeansMap.size()]);
        }
    }


    /**
     * Register the provided object with the {@link MonitorRegistry}
     *
     * @param namespace - which space to register
     * @param obj       - object annotated w/ {@link Monitor}
     * @throws InstanceNotFoundException
     */
    public void registerObject(Namespace namespace, Object obj) {
        try {
            if (obj == null || namespace == null) {
                return;
            }
            MonitoredResource mbean = new MonitoredResource(namespace, obj);
            ObjectName name = mbean.getObjectName();
            if (_mbeanServer.isRegistered(name)) {
                LOGGER.warn("Overriding mbean w/ ObjectName: " + name);
                _mbeanServer.unregisterMBean(name);
            }
            _mbeanServer.registerMBean(mbean, name);
            if (namespace == Namespace.APPLICATION) {
                _appMBeansMap.put(obj, name);
            } else {
                _platformMBeansMap.put(obj, name);
            }
        } catch (Throwable t) {
            LOGGER.warn("Error while registering mbean obj:"
                    + obj
                    + "This should not affect your normal operation (unless your operation depends on the Monitored MBean)", t);
        }
    }

    /**
     * Should unregister when obj is no longer needed
     */
    public void unRegisterObject(Object obj) {
        try {
            ObjectName name = _platformMBeansMap.get(obj);
            if (name == null) {
                name = _appMBeansMap.get(obj);
            }
            if (name != null) {
                LOGGER.info("Unregistering with MonitorRegistry: " + name);
                _mbeanServer.unregisterMBean(name);
            }
        } catch (InstanceNotFoundException e) {
            //ignore
        } catch (Throwable t) {
            LOGGER.error("", t);
        }
    }

    /**
     * Register the provided object with the {@link MonitorRegistry} in the
     * {@link Namespace#APPLICATION} space.
     *
     * @param obj - object annotated w/ {@link Monitor}
     */
    public void registerObject(Object obj) {
        registerObject(Namespace.APPLICATION, obj);
    }


}
