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
package com.netflix.servo;

import com.google.common.collect.Sets;
import com.netflix.servo.jmx.JmxMonitorRegistry;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;
import org.testng.annotations.Test;

import java.util.Properties;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DefaultMonitorRegistryTest {

/*    private Properties getProps() {
        Properties props = new Properties();
        props.setProperty(
            "com.netflix.servo.DefaultMonitorRegistry.registryClass",
            "com.netflix.servo.BasicMonitorRegistry");
        return props;
    }

    private DefaultMonitorRegistry newInstance() {
        return new DefaultMonitorRegistry(getProps());
    }

    private Set<Object> getObjects(MonitorRegistry registry) {
        Set<AnnotatedObject> annoObjs = registry.getRegisteredAnnotatedObjects();
        Set<Object> objects = Sets.newHashSet();
        for (AnnotatedObject annoObj : annoObjs) {
            objects.add(annoObj.getObject());
        }
        return objects;
    }

    @Test
    public void testInvalidClass() throws Exception {
        Properties props = new Properties();
        props.setProperty(
            "com.netflix.servo.DefaultMonitorRegistry.registryClass",
            "com.netflix.servo.BadClass");
        DefaultMonitorRegistry registry =
            new DefaultMonitorRegistry(props);
        MonitorRegistry innerReg = registry.getInnerRegistry();
        assertEquals(innerReg.getClass(), JmxMonitorRegistry.class);
    }

    @Test
    public void testCorrectInnerTypeCreated() throws Exception {
        DefaultMonitorRegistry registry = newInstance();
        MonitorRegistry innerReg = registry.getInnerRegistry();
        assertEquals(innerReg.getClass(), BasicMonitorRegistry.class);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testRegisterNull() throws Exception {
        DefaultMonitorRegistry registry = newInstance();
        registry.registerAnnotatedObject(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testUnRegisterNull() throws Exception {
        DefaultMonitorRegistry registry = newInstance();
        registry.unregisterAnnotatedObject(null);
    }

    @Test
    public void testGetRegisteredObjectsEmpty() throws Exception {
        DefaultMonitorRegistry registry = newInstance();
        Set<Object> objects = getObjects(registry);
        assertEquals(objects.size(), 0);
    }

    @Test
    public void testGetRegisteredObjects() throws Exception {
        DefaultMonitorRegistry registry = newInstance();
        Object o1 = new BasicCounter("one");
        Object o2 = new BasicCounter("two");

        registry.registerAnnotatedObject(o1);
        registry.registerAnnotatedObject(o2);

        Set<Object> objects = getObjects(registry);
        assertEquals(objects.size(), 2);
        assertTrue(objects.contains(o1));
        assertTrue(objects.contains(o2));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testRegisterMonitorNull() throws Exception {
        DefaultMonitorRegistry registry = newInstance();
        registry.register(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testUnRegisterMonitorNull() throws Exception {
        DefaultMonitorRegistry registry = newInstance();
        registry.unregister(null);
    }

    @Test
    public void testGetRegisteredMonitorsEmpty() throws Exception {
        DefaultMonitorRegistry registry = newInstance();
        Set<Monitor> monitors = registry.getRegisteredMonitors();
        assertEquals(monitors.size(), 0);
    }

    @Test
    public void testGetRegisteredMonitors() throws Exception {
        DefaultMonitorRegistry registry = newInstance();
        Monitor m1 = new com.netflix.servo.monitor.BasicCounter(new MonitorConfig.Builder("test1").build());
        Monitor m2 = new com.netflix.servo.monitor.BasicCounter(new MonitorConfig.Builder("test2").build());

        registry.register(m1);
        registry.register(m2);

        Set<Monitor> monitors = registry.getRegisteredMonitors();
        assertEquals(monitors.size(), 2);
        assertTrue(monitors.contains(m1));
        assertTrue(monitors.contains(m2));
    }*/
}
