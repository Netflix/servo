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

public class MonitoredResource implements DynamicMBean {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MonitoredResource.class);

    private final Object mObject;

    private final ObjectName mName;

    private final MBeanInfo mBeanInfo;

    private final Map<String,MonitoredAttribute> mAttrs;

    private final MetadataMBean mMetadataMBean;

    public MonitoredResource(Object obj) {
        this(null, obj);
    }

    public MonitoredResource(String domain, Object obj) {
        mObject = Preconditions.checkNotNull(obj, "object cannot be null");

        String className = mObject.getClass().getCanonicalName();
        String id;
        try {
            id = AnnotationUtils.getMonitorId(mObject);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format(
                "could not get monitor id from object of type %s",
                className), e);
        }

        mName = createObjectName(domain, className, id, "value");

        ImmutableMap.Builder<String,MonitoredAttribute> attrs =
            ImmutableMap.builder();
        List<MonitoredAttribute> monitoredAttrs =
            AnnotationUtils.getMonitoredAttributes(obj);

        MBeanAttributeInfo[] attributes =
            new MBeanAttributeInfo[monitoredAttrs.size()];
        for (int i = 0; i < monitoredAttrs.size(); ++i) {
            MonitoredAttribute attr = monitoredAttrs.get(i);
            Monitor m = attr.annotation();
            attrs.put(m.name(), attr);
            attributes[i] = attr.valueAttributeInfo();
        }
        mAttrs = attrs.build();

        mBeanInfo = new MBeanInfo(
            className,
            "MonitoredResource MBean",
            attributes,  // attributes
            null,  // constructors
            null,  // operations
            null); // notifications

        ObjectName metadataName =
            createObjectName(domain, className, id, "metadata");
        MBeanInfo metadataInfo = new MBeanInfo(
            className,
            "MonitoredResource Metdata MBean",
            attributes,  // attributes
            null,  // constructors
            null,  // operations
            null); // notifications
        mMetadataMBean = new MetadataMBean(metadataName, metadataInfo, mAttrs);
    }

    private ObjectName createObjectName(
            String domain, String className, String id, String field) {
        StringBuilder buf = new StringBuilder();
        buf.append((domain == null) ? getClass().getCanonicalName() : domain)
           .append(":class=")
           .append(className);
        if (id != null) {
            buf.append(",instance=").append(id);
        }
        buf.append(",field=").append(field);

        String name = buf.toString();
        try {
            return new ObjectName(buf.toString());
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("invalid ObjectName " + name, e);
        }
    }

    public ObjectName getObjectName() {
        return mName;
    }

    public MetadataMBean getMetadataMBean() {
        return mMetadataMBean;
    }

    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException {
        MonitoredAttribute attr = mAttrs.get(attribute);
        if (attr == null) {
            throw new AttributeNotFoundException(attribute);
        }
        try {
            return attr.value();
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
