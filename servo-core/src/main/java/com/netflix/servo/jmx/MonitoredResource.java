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
import com.google.common.collect.ImmutableMap;
import com.netflix.servo.annotations.AnnotatedAttribute;
import com.netflix.servo.annotations.AnnotatedObject;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.util.List;
import java.util.Map;

public final class MonitoredResource implements DynamicMBean {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MonitoredResource.class);

    private final AnnotatedObject object;

    private final ObjectName name;

    private final MBeanInfo beanInfo;

    private final Map<String,MonitoredAttribute> attrs;

    private final MetadataMBean metadataMBean;

    public MonitoredResource(AnnotatedObject obj) {
        this(null, obj);
    }

    public MonitoredResource(String domain, AnnotatedObject obj) {
        object = Preconditions.checkNotNull(obj, "object cannot be null");

        String className = object.getClassName();
        name = createObjectName(domain, className, object.getTags(), "value");

        ImmutableMap.Builder<String,MonitoredAttribute> builder =
            ImmutableMap.builder();
        List<AnnotatedAttribute> annotatedAttrs = obj.getAttributes();

        MBeanAttributeInfo[] attributes =
            new MBeanAttributeInfo[annotatedAttrs.size()];
        for (int i = 0; i < annotatedAttrs.size(); ++i) {
            MonitoredAttribute attr = new MonitoredAttribute(
                annotatedAttrs.get(i));
            Monitor m = attr.getAnnotation();
            builder.put(m.name(), attr);
            attributes[i] = attr.getValueAttributeInfo();
        }
        attrs = builder.build();

        beanInfo = new MBeanInfo(
            className,
            "MonitoredResource MBean",
            attributes,  // attributes
            null,  // constructors
            null,  // operations
            null); // notifications

        ObjectName metadataName =
            createObjectName(domain, className, object.getTags(), "metadata");
        MBeanInfo metadataInfo = new MBeanInfo(
            className,
            "MonitoredResource Metdata MBean",
            attributes,  // attributes
            null,  // constructors
            null,  // operations
            null); // notifications
        metadataMBean = new MetadataMBean(metadataName, metadataInfo, attrs);
    }

    private ObjectName createObjectName(String domain, String className, TagList tags, String field) {
        StringBuilder buf = new StringBuilder();
        buf.append((domain == null) ? getClass().getCanonicalName() : domain)
           .append(":class=")
           .append(className);

        for(Tag t : tags){
            buf.append(",").append(t.tagString());
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
        return name;
    }

    public MetadataMBean getMetadataMBean() {
        return metadataMBean;
    }

    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException {
        MonitoredAttribute attr = attrs.get(attribute);
        if (attr == null) {
            throw new AttributeNotFoundException(attribute);
        }
        try {
            return attr.getValue();
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
        return beanInfo;
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
