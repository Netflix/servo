/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.jmx;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import com.netflix.servo.monitor.CompositeMonitor;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.NumericMonitor;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;

import java.util.List;
import java.util.regex.Pattern;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * Exposes a {@link com.netflix.servo.monitor.Monitor} as an MBean that can be registered with JMX.
 */
class MonitorMBean implements DynamicMBean {

    /**
     * Create a set of MBeans for a {@link com.netflix.servo.monitor.Monitor}. This method will
     * recursively select all of the sub-monitors if a composite type is used.
     *
     * @param domain   passed in to the object name created to identify the beans
     * @param monitor  monitor to expose to jmx
     * @return         flattened list of simple monitor mbeans
     */
    public static List<MonitorMBean> createMBeans(String domain, Monitor<?> monitor) {
        List<MonitorMBean> mbeans = Lists.newArrayList();
        createMBeans(mbeans, domain, monitor);
        return mbeans;
    }

    private static void createMBeans(List<MonitorMBean> mbeans, String domain, Monitor<?> monitor) {
        if (monitor instanceof CompositeMonitor<?>) {
            for (Monitor<?> m : ((CompositeMonitor<?>) monitor).getMonitors()) {
                createMBeans(mbeans, domain, m);
            }
        } else {
            mbeans.add(new MonitorMBean(domain, monitor));
        }
    }

    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_\\-\\.]");

    private final Monitor<?> monitor;

    private final ObjectName objectName;

    private final MBeanInfo beanInfo;

    /**
     * Create an MBean for a {@link com.netflix.servo.monitor.Monitor}.
     *
     * @param domain   passed in to the object name created to identify the beans
     * @param monitor  monitor to expose to jmx
     */
    MonitorMBean(String domain, Monitor<?> monitor) {
        this.monitor = monitor;
        this.objectName = createObjectName(domain);
        this.beanInfo = createBeanInfo();
    }

    /**
     * Returns the object name built from the {@link com.netflix.servo.monitor.MonitorConfig}.
     */
    public ObjectName getObjectName() {
        return objectName;
    }

    /** {@inheritDoc} */
    @Override
    public Object getAttribute(String name) throws AttributeNotFoundException {
        return monitor.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public void setAttribute(Attribute attribute)
            throws InvalidAttributeValueException, MBeanException, AttributeNotFoundException {
        throw new UnsupportedOperationException("setAttribute is not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public AttributeList getAttributes(String[] names) {
        AttributeList list = new AttributeList();
        for (String name : names) {
            list.add(new Attribute(name, monitor.getValue()));
        }
        return list;
    }

    /** {@inheritDoc} */
    @Override
    public AttributeList setAttributes(AttributeList list) {
        throw new UnsupportedOperationException("setAttributes is not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public Object invoke(String name, Object[] args, String[] sig)
            throws MBeanException, ReflectionException {
        throw new UnsupportedOperationException("invoke is not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public MBeanInfo getMBeanInfo() {
        return beanInfo;
    }

    private ObjectName createObjectName(String domain) {
        try {
            final String name = monitor.getConfig().getName();
            final String sanitizedDomain = INVALID_CHARS.matcher(domain).replaceAll("_");
            final String sanitizedName = INVALID_CHARS.matcher(name).replaceAll("_");
            StringBuilder builder = new StringBuilder();
            builder.append(sanitizedDomain).append(':');
            builder.append("name=").append(sanitizedName);

            TagList tags = monitor.getConfig().getTags();
            for (Tag tag : tags) {
                final String sanitizedKey = INVALID_CHARS.matcher(tag.getKey()).replaceAll("_");
                final String sanitizedValue = INVALID_CHARS.matcher(tag.getValue()).replaceAll("_");
                builder.append(',').append(sanitizedKey).append('=').append(sanitizedValue);
            }
            return new ObjectName(builder.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw Throwables.propagate(e);
        }
    }

    private MBeanInfo createBeanInfo() {
        MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[1];
        attrs[0] = createAttributeInfo(monitor);
        return new MBeanInfo(
                this.getClass().getName(),
                "MonitorMBean",
                attrs,
                null,  // constructors
                null,  // operators
                null); // notifications
    }

    private MBeanAttributeInfo createAttributeInfo(Monitor<?> m) {
        final String type = (m instanceof NumericMonitor<?>)
            ? Number.class.getName()
            : String.class.getName();
        return new MBeanAttributeInfo(
            "value",
            type,
            m.getConfig().toString(),
            true,   // isReadable
            false,  // isWritable
            false); // isIs
    }
}
