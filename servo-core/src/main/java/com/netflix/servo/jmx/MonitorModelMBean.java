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
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.Tag;

import javax.management.*;
import javax.management.modelmbean.*;
import java.lang.reflect.Method;

class MonitorModelMBean {
    private final ObjectName objectName;
    private final ModelMBean mBean;

    private MonitorModelMBean(String registry, Monitor monitor) {
        this.objectName = createObjectName(registry, monitor.getConfig());
        try {
            this.mBean = createModelMBean(monitor);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not create Mbean for Monitor:" + monitor, e);
        }
    }

    static MonitorModelMBean newInstance(String registry, Monitor monitor) {
        if (monitor == null) throw new IllegalArgumentException("Monitor cannot be null");
        return new MonitorModelMBean(registry, monitor);
    }

    ObjectName getObjectName() {
        return objectName;
    }

    ModelMBean getMBean() {
        return mBean;
    }

    static ObjectName createObjectName(String domain, MonitorConfig config) {
        Preconditions.checkNotNull(config, "MonitorConfig cannot be null");

        StringBuilder builder = new StringBuilder();
        builder.append(domain != null ? domain : "com.netflix.servo").append(":name=").append(config.getName());

        for (Tag t : config.getTags()) {
            builder.append(",").append(t.tagString());
        }

        String name = builder.toString();
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("invalid ObjectName " + name, e);
        }
    }

    private ModelMBean createModelMBean(Monitor monitor) throws IntrospectionException, NoSuchMethodException,
            MBeanException, InstanceNotFoundException, InvalidTargetObjectTypeException {
        RequiredModelMBean monitorMMBean = new RequiredModelMBean(createModelMBeanInfo(monitor));
        monitorMMBean.setManagedResource(monitor, "ObjectReference");
        return monitorMMBean;
    }

    private ModelMBeanInfo createModelMBeanInfo(Monitor monitor) throws MBeanException, IntrospectionException,
            NoSuchMethodException {
        Class<?> monitorClass = monitor.getClass();
        Method getValue = monitorClass.getMethod("getValue");

        Descriptor monitorDescription = new DescriptorSupport("name=" + objectName, "descriptorType=mbean",
                "displayName=" + monitor.getConfig().getName(), "type=" + monitorClass.getCanonicalName(),
                "log=T", "logFile=jmxmain.log", "currencyTimeLimit=10");

        ModelMBeanInfo info = new ModelMBeanInfoSupport("Monitor", "ModelMBean for Monitor objects",
                createMonitorAttributes(monitorClass, getValue),
                null,
                createMonitorOperations(monitorClass, getValue),
                null);

        info.setMBeanDescriptor(monitorDescription);

        return info;
    }

    /**
     * This method only supports building operations for no arugment methods.
     *
     * @param monitorClass
     * @param methods
     * @return
     */
    private ModelMBeanOperationInfo[] createMonitorOperations(Class monitorClass, Method... methods) {
        ModelMBeanOperationInfo[] monitorOperations = new ModelMBeanOperationInfo[methods.length];

        MBeanParameterInfo[] getParms = new MBeanParameterInfo[0];

        for (int i = 0; i < methods.length; i++) {
            Descriptor getValueDesc = new DescriptorSupport("name=" + methods[i].getName(), "descriptorType=operation",
                    "class=" + monitorClass.getCanonicalName(), "role=operation");

            monitorOperations[i] = new ModelMBeanOperationInfo(methods[i].getName(), methods[i].getName(), getParms,
                    methods[i].getReturnType().getCanonicalName(), MBeanOperationInfo.INFO, getValueDesc);
        }
        return monitorOperations;
    }

    private ModelMBeanAttributeInfo[] createMonitorAttributes(Class monitorClass, Method... methods) {
        ModelMBeanAttributeInfo[] attributes = new ModelMBeanAttributeInfo[methods.length];

        for (int i = 0; i < methods.length; i++) {
            Descriptor monitorValueDescription = new DescriptorSupport("name=" + methods[i].getName(),
                    "descriptorType=attribute", "displayName=" + methods[i].getName(),
                    "getMethod=" + methods[i].getName());

            attributes[i] = new ModelMBeanAttributeInfo(methods[i].getName(), methods[i].getReturnType().getCanonicalName(),
                    "Current value of the monitor", true, false, false, monitorValueDescription);
        }
        return attributes;
    }
}
