/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 - 2012 Netflix
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

import com.google.common.collect.ImmutableMap;

import com.netflix.servo.BasicTagList;
import com.netflix.servo.TagList;
import com.netflix.servo.MonitorRegistry;

import com.netflix.servo.util.BasicCounter;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class JmxMonitorRegistryTest {

    private JmxMonitorRegistry newInstance() {
        return new JmxMonitorRegistry();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testRegisterNull() throws Exception {
        MonitorRegistry registry = newInstance();
        registry.registerObject(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testUnRegisterNull() throws Exception {
        MonitorRegistry registry = newInstance();
        registry.unRegisterObject(null);
    }

    @Test
    public void testUnRegisterObject() throws Exception {
        MonitorRegistry registry = newInstance();
        Object o1 = new BasicCounter("one");
        Object o2 = new BasicCounter("two");

        registry.registerObject(o1);
        registry.registerObject(o2);

        Set<Object> objects = registry.getRegisteredObjects();
        assertEquals(objects.size(), 2);
        assertTrue(objects.contains(o1));
        assertTrue(objects.contains(o2));

        registry.unRegisterObject(o1);
        objects = registry.getRegisteredObjects();
        assertEquals(objects.size(), 1);
        assertFalse(objects.contains(o1));
        assertTrue(objects.contains(o2));

        registry.unRegisterObject(o2);
        objects = registry.getRegisteredObjects();
        assertEquals(objects.size(), 0);
        assertFalse(objects.contains(o1));
        assertFalse(objects.contains(o2));
    }

    @Test
    public void testGetRegisteredObjectsEmpty() throws Exception {
        MonitorRegistry registry = newInstance();
        Set<Object> objects = registry.getRegisteredObjects();
        assertEquals(objects.size(), 0);
    }

    @Test
    public void testGetRegisteredObjects() throws Exception {
        MonitorRegistry registry = newInstance();
        Object o1 = new BasicCounter("one");
        Object o2 = new BasicCounter("two");

        registry.registerObject(o1);
        registry.registerObject(o2);

        Set<Object> objects = registry.getRegisteredObjects();
        assertEquals(objects.size(), 2);
        assertTrue(objects.contains(o1));
        assertTrue(objects.contains(o2));
    }
}
