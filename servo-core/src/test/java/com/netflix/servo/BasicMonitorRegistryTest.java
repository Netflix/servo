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
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.BasicCounter;
import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.Assert.*;

public class BasicMonitorRegistryTest {
/*
    private BasicMonitorRegistry newInstance() {
        return new BasicMonitorRegistry();
    }

    private Set<Object> getObjects(MonitorRegistry registry) {
        Set<AnnotatedObject> annoObjs = registry.getRegisteredAnnotatedObjects();
        Set<Object> objects = Sets.newHashSet();
        for (AnnotatedObject annoObj : annoObjs) {
            objects.add(annoObj.getObject());
        }
        return objects;
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testRegisterNull() throws Exception {
        MonitorRegistry registry = newInstance();
        registry.registerAnnotatedObject(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testUnRegisterNull() throws Exception {
        MonitorRegistry registry = newInstance();
        registry.unregisterAnnotatedObject(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testRegisterNullMonitor() throws Exception {
        MonitorRegistry registry = newInstance();
        registry.register(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testUnRegisterNullMonitor() throws Exception {
        MonitorRegistry registry = newInstance();
        registry.unregister(null);
    }

    @Test
    public void testUnRegisterObject() throws Exception {
        MonitorRegistry registry = newInstance();
        Object o1 = new BasicCounter("one");
        Object o2 = new BasicCounter("two");

        registry.registerAnnotatedObject(o1);
        registry.registerAnnotatedObject(o2);

        Set<Object> objects = getObjects(registry);
        assertEquals(objects.size(), 2);
        assertTrue(objects.contains(o1));
        assertTrue(objects.contains(o2));

        registry.unregisterAnnotatedObject(o1);
        objects = getObjects(registry);
        assertEquals(objects.size(), 1);
        assertFalse(objects.contains(o1));
        assertTrue(objects.contains(o2));

        registry.unregisterAnnotatedObject(o2);
        objects = getObjects(registry);
        assertEquals(objects.size(), 0);
        assertFalse(objects.contains(o1));
        assertFalse(objects.contains(o2));
    }

    @Test
    public void testGetRegisteredObjectsEmpty() throws Exception {
        MonitorRegistry registry = newInstance();
        Set<Object> objects = getObjects(registry);
        assertEquals(objects.size(), 0);
    }

    @Test
    public void testGetRegisteredObjects() throws Exception {
        MonitorRegistry registry = newInstance();
        Object o1 = new BasicCounter("one");
        Object o2 = new BasicCounter("two");

        registry.registerAnnotatedObject(o1);
        registry.registerAnnotatedObject(o2);

        Set<Object> objects = getObjects(registry);
        assertEquals(objects.size(), 2);
        assertTrue(objects.contains(o1));
        assertTrue(objects.contains(o2));
    }

    @Test
    public void testUnRegisterMonitor() throws Exception {
        MonitorRegistry registry = newInstance();
        Monitor m1 = new com.netflix.servo.monitor.BasicCounter(new MonitorConfig.Builder("test1").build());
        Monitor m2 = new com.netflix.servo.monitor.BasicCounter(new MonitorConfig.Builder("test2").build());

        registry.register(m1);
        registry.register(m2);

        Set<Monitor> monitors = registry.getRegisteredMonitors();
        assertEquals(monitors.size(), 2);
        assertTrue(monitors.contains(m1));
        assertTrue(monitors.contains(m2));

        registry.unregister(m1);
        monitors = registry.getRegisteredMonitors();
        assertEquals(monitors.size(), 1);
        assertFalse(monitors.contains(m1));
        assertTrue(monitors.contains(m2));

        registry.unregister(m2);
        monitors = registry.getRegisteredMonitors();
        assertEquals(monitors.size(), 0);
        assertFalse(monitors.contains(m1));
        assertFalse(monitors.contains(m2));
    }

    @Test
    public void testGetRegisteredMonitorsEmpty() throws Exception {
        MonitorRegistry registry = newInstance();
        Set<Monitor> monitors = registry.getRegisteredMonitors();
        assertEquals(monitors.size(), 0);
    }

    @Test
    public void testGetRegisteredMonitors() throws Exception {
        MonitorRegistry registry = newInstance();
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
