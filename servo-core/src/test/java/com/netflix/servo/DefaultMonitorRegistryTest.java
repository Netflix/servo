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
package com.netflix.servo;

import com.netflix.servo.jmx.ObjectNameMapper;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;
import org.testng.annotations.Test;

import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DefaultMonitorRegistryTest {
  @Test
  public void testCustomJmxObjectMapper() {
    Properties props = new Properties();
    props.put("com.netflix.servo.DefaultMonitorRegistry.jmxMapperClass",
        "com.netflix.servo.DefaultMonitorRegistryTest$ChangeDomainMapper");
    DefaultMonitorRegistry registry = new DefaultMonitorRegistry(props);
    BasicCounter counter = new BasicCounter(
        new MonitorConfig.Builder("testCustomJmxObjectMapper").build());
    registry.register(counter);
    ObjectName expectedName =
        new ChangeDomainMapper().createObjectName("com.netflix.servo", counter);
    assertEquals(expectedName.getDomain(), "com.netflix.servo.Renamed");
    assertTrue(ManagementFactory.getPlatformMBeanServer().isRegistered(expectedName));
  }

  @Test
  public void testInvalidMapperDefaults() {
    Properties props = new Properties();
    props.put("com.netflix.servo.DefaultMonitorRegistry.jmxMapperClass",
        "com.my.invalid.class");
    DefaultMonitorRegistry registry = new DefaultMonitorRegistry(props);
    BasicCounter counter = new BasicCounter(
        new MonitorConfig.Builder("testInvalidMapperDefaults").build());
    registry.register(counter);
    ObjectName expectedName =
        ObjectNameMapper.DEFAULT.createObjectName("com.netflix.servo", counter);
    assertEquals(expectedName.getDomain(), "com.netflix.servo");
    assertTrue(ManagementFactory.getPlatformMBeanServer().isRegistered(expectedName));
  }

  public static class ChangeDomainMapper implements ObjectNameMapper {
    @Override
    public ObjectName createObjectName(String domain, Monitor<?> monitor) {
      return ObjectNameMapper.DEFAULT.createObjectName(domain + ".Renamed", monitor);
    }

  }
}
