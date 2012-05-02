package com.netflix.servo.jmx;

import com.netflix.servo.Monitor;
import com.netflix.servo.MonitorContext;
import com.netflix.servo.tag.Tag;

import javax.management.*;
import java.util.Map;

/**
 * User: gorzell
 * Date: 5/1/12
  */
public final class MonitorMBean implements DynamicMBean {
    private final Monitor monitor;
    private final ObjectName objectName;
    private final MBeanInfo beanInfo;
    private final Map<String, MonitoredAttribute> attrs;
    private final MetadataMBean metadataMBean;

    public MonitorMBean(Monitor monitor){
        this.monitor = monitor;
        this.objectName = createObjectName(monitor.getContext());
    }

    private ObjectName createObjectName(MonitorContext context){
        StringBuilder builder = new StringBuilder();
        builder.append((domain == null) ? getClass().getCanonicalName() : domain)
                .append(":class=")
                .append(className);

        for (Tag t : tags) {
            builder.append(",").append(t.tagString());
        }

        builder.append(",field=").append(field);

        String name = builder.toString();
        try {
            return new ObjectName(builder.toString());
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("invalid ObjectName " + name, e);
        }
    }

    @Override
    public Object getAttribute(String s) throws AttributeNotFoundException, MBeanException, ReflectionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AttributeList getAttributes(String[] strings) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    //Unsupported methods
    @Override
    public Object invoke(String s, Object[] objects, String[] strings) throws MBeanException, ReflectionException {
        throw new UnsupportedOperationException("invoke(...) is not supported on this mbean");
    }

    @Override
    public AttributeList setAttributes(AttributeList objects) {
        throw new UnsupportedOperationException("setAttributes(...) is not supported on this mbean");
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        throw new UnsupportedOperationException("setAttribute(...) is not supported on this mbean");
    }
}
