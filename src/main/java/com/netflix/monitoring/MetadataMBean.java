package com.netflix.monitoring;

import com.google.common.base.Preconditions;

import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;

import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.SimpleType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MetadataMBean implements DynamicMBean {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MetadataMBean.class);

    private final ObjectName mName;

    private final MBeanInfo mBeanInfo;

    private final Map<String,MonitoredAttribute> mAttrs;

    MetadataMBean(
            ObjectName name,
            MBeanInfo beanInfo,
            Map<String,MonitoredAttribute> attrs) {
        mName = name;
        mBeanInfo = beanInfo;
        mAttrs = attrs;
    }

    public ObjectName getObjectName() {
        return mName;
    }

    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException {
        MonitoredAttribute attr = mAttrs.get(attribute);
        if (attr == null) {
            throw new AttributeNotFoundException(attribute);
        }
        try {
            return attr.metadata();
        } catch (Exception e) {
            throw new MBeanException(e);
        }
    }

    public AttributeList getAttributes(String[] attributes) {
        AttributeList list = new AttributeList();
        for (String a : attributes) {
            try {
                list.add(new Attribute(a, getAttribute(a)));
            } catch (Exception e) {
                LOGGER.warn("getAttribute() failed for " + a, e);
            }
        }
        return list;
    }

    public MBeanInfo getMBeanInfo() {
        return mBeanInfo;
    }

    public Object invoke(
            String actionName, Object[] params, String[] signature) {
        throw new UnsupportedOperationException(
            "invoke(...) is not supported on this mbean");
    }

    public void setAttribute(Attribute attribute) {
        throw new UnsupportedOperationException(
            "setAttribute(...) is not supported on this mbean");
    }

    public AttributeList setAttributes(AttributeList attributes) {
        throw new UnsupportedOperationException(
            "setAttributes(...) is not supported on this mbean");
    }
}
