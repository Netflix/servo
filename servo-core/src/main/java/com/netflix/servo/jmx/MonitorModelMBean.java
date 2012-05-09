package com.netflix.servo.jmx;

import com.netflix.servo.Monitor;
import com.netflix.servo.MonitorContext;
import com.netflix.servo.tag.Tag;

import javax.management.*;
import javax.management.modelmbean.*;
import java.lang.reflect.Method;

/**
 * User: gorzell
 * Date: 5/8/12
 */
class MonitorModelMBean {
    private final ObjectName objectName;
    private final ModelMBean mBean;

    private MonitorModelMBean(Monitor monitor) {
        this.objectName = createObjectName(monitor.getContext());
        try {
            this.mBean = createModelMBean(monitor);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not create Mbean for Monitor:" + monitor, e);
        }
    }

    public static MonitorModelMBean newInstance(Monitor monitor) {
        return new MonitorModelMBean(monitor);
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public ModelMBean getMBean() {
        return mBean;
    }

    static ObjectName createObjectName(MonitorContext context) {
        StringBuilder builder = new StringBuilder();
        builder.append("com.netflix.servo").append(":name=").append(context.getName());

        for (Tag t : context.getTags()) {
            builder.append(",").append(t.tagString());
        }

        String name = builder.toString();
        try {
            return new ObjectName(builder.toString());
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("invalid ObjectName " + name, e);
        }
    }

    private ModelMBean createModelMBean(Monitor monitor) throws IntrospectionException, NoSuchMethodException, MBeanException, InstanceNotFoundException, InvalidTargetObjectTypeException {
        RequiredModelMBean monitorMMBean = new RequiredModelMBean(createModelMBeanInfo(monitor));
        monitorMMBean.setManagedResource(monitor, "ObjectReference");
        return monitorMMBean;
    }

    private ModelMBeanInfo createModelMBeanInfo(Monitor monitor) throws MBeanException, IntrospectionException, NoSuchMethodException {
        Class monitorClass = monitor.getClass();
        Method m = monitorClass.getMethod("getValue", null);
        System.out.println(m.getReturnType().getCanonicalName());


        Descriptor monitorDescription = new DescriptorSupport("name=" + objectName,
                "descriptorType=mbean",
                "displayName=" + monitor.getContext().getName(),
                "type=" + monitorClass.getCanonicalName(),
                "log=T",
                "logFile=jmxmain.log",
                "currencyTimeLimit=10");

        ModelMBeanInfo info = new ModelMBeanInfoSupport("Monitor", "ModelMBean for Monitor objects",
                createMonitorAttributes(monitorClass, m),
                null,
                createMonitorOperations(monitorClass, m),
                null);

        info.setMBeanDescriptor(monitorDescription);

        return info;
    }

    private ModelMBeanOperationInfo[] createMonitorOperations(Class monitorClass, Method... methods) {
        ModelMBeanOperationInfo[] monitorOperations =
                new ModelMBeanOperationInfo[methods.length];

        MBeanParameterInfo[] getParms = new MBeanParameterInfo[0];

        for (int i = 0; i < methods.length; i++) {
            //TODO dynamic naming
            Descriptor getValueDesc =
                    new DescriptorSupport(
                            "name=" + methods[i].getName(),
                            "descriptorType=operation",
                            "class=" + monitorClass.getCanonicalName(),
                            "role=operation");

            monitorOperations[0] =
                    new ModelMBeanOperationInfo(
                            methods[i].getName(),
                            methods[i].getName(),
                            getParms,
                            methods[i].getReturnType().getCanonicalName(),
                            MBeanOperationInfo.INFO,
                            getValueDesc);
        }
        return monitorOperations;
    }

    private ModelMBeanAttributeInfo[] createMonitorAttributes(Class monitorClass, Method... methods) {
        ModelMBeanAttributeInfo[] attributes = new ModelMBeanAttributeInfo[methods.length];

        for (int i = 0; i < methods.length; i++) {
            //TODO Dynamic naming
            Descriptor monitorValueDescription = new DescriptorSupport("name=MonitorValue",
                    "descriptorType=attribute",
                    "displayName=MonitorValue",
                    "getMethod=" + methods[i].getName());

            attributes[i] = new ModelMBeanAttributeInfo("MonitorValue", methods[i].getReturnType().getCanonicalName(),
                    "Current value of the monitor", true, false, false, monitorValueDescription);
        }
        return attributes;
    }
}
