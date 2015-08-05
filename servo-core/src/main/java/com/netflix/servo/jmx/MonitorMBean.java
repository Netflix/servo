/**
 * Copyright 2013 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.jmx;

import com.netflix.servo.monitor.CompositeMonitor;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.NumericMonitor;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Exposes a {@link com.netflix.servo.monitor.Monitor} as an MBean that can be registered with JMX.
 */
class MonitorMBean implements DynamicMBean {

  /**
   * Create a set of MBeans for a {@link com.netflix.servo.monitor.Monitor}. This method will
   * recursively select all of the sub-monitors if a composite type is used.
   *
   * @param domain  passed in to the object name created to identify the beans
   * @param monitor monitor to expose to jmx
   * @param mapper  the mapper which maps the Monitor to ObjectName
   * @return flattened list of simple monitor mbeans
   */
  public static List<MonitorMBean> createMBeans(String domain, Monitor<?> monitor,
                                                ObjectNameMapper mapper) {
    List<MonitorMBean> mbeans = new ArrayList<>();
    createMBeans(mbeans, domain, monitor, mapper);
    return mbeans;
  }

  private static void createMBeans(List<MonitorMBean> mbeans, String domain, Monitor<?> monitor,
                                   ObjectNameMapper mapper) {
    if (monitor instanceof CompositeMonitor<?>) {
      for (Monitor<?> m : ((CompositeMonitor<?>) monitor).getMonitors()) {
        createMBeans(mbeans, domain, m, mapper);
      }
    } else {
      mbeans.add(new MonitorMBean(domain, monitor, mapper));
    }
  }

  private final Monitor<?> monitor;

  private final ObjectName objectName;

  private final MBeanInfo beanInfo;

  /**
   * Create an MBean for a {@link com.netflix.servo.monitor.Monitor}.
   *
   * @param domain  passed in to the object name created to identify the beans
   * @param monitor monitor to expose to jmx
   * @param mapper  the mapper which maps the monitor to ObjectName
   */
  MonitorMBean(String domain, Monitor<?> monitor, ObjectNameMapper mapper) {
    this.monitor = monitor;
    this.objectName = createObjectName(mapper, domain);
    this.beanInfo = createBeanInfo();
  }

  /**
   * Returns the object name built from the {@link com.netflix.servo.monitor.MonitorConfig}.
   */
  public ObjectName getObjectName() {
    return objectName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getAttribute(String name) throws AttributeNotFoundException {
    return monitor.getValue();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAttribute(Attribute attribute)
      throws InvalidAttributeValueException, MBeanException, AttributeNotFoundException {
    throw new UnsupportedOperationException("setAttribute is not implemented");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AttributeList getAttributes(String[] names) {
    AttributeList list = new AttributeList();
    for (String name : names) {
      list.add(new Attribute(name, monitor.getValue()));
    }
    return list;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AttributeList setAttributes(AttributeList list) {
    throw new UnsupportedOperationException("setAttributes is not implemented");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object invoke(String name, Object[] args, String[] sig)
      throws MBeanException, ReflectionException {
    throw new UnsupportedOperationException("invoke is not implemented");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MBeanInfo getMBeanInfo() {
    return beanInfo;
  }

  private ObjectName createObjectName(ObjectNameMapper mapper, String domain) {
    return mapper.createObjectName(domain, monitor);
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
