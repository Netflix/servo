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
package com.netflix.servo;

import com.google.common.collect.ImmutableMap;

import com.netflix.servo.BasicTagList;
import com.netflix.servo.TagList;

import com.netflix.servo.jmx.JmxMonitorRegistry;

import com.netflix.servo.util.BasicCounter;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class DefaultMonitorRegistryTest {

    private Properties getProps() {
        Properties props = new Properties();
        props.setProperty(
            "com.netflix.servo.DefaultMonitorRegistry.registryClass",
            "com.netflix.servo.BasicMonitorRegistry");
        return props;
    }

    private DefaultMonitorRegistry newInstance() {
        return new DefaultMonitorRegistry(getProps());
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
        registry.registerObject(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testUnRegisterNull() throws Exception {
        DefaultMonitorRegistry registry = newInstance();
        registry.unRegisterObject(null);
    }

    @Test
    public void testGetRegisteredObjectsEmpty() throws Exception {
        DefaultMonitorRegistry registry = newInstance();
        Set<Object> objects = registry.getRegisteredObjects();
        assertEquals(objects.size(), 0);
    }

    @Test
    public void testGetRegisteredObjects() throws Exception {
        DefaultMonitorRegistry registry = newInstance();
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
