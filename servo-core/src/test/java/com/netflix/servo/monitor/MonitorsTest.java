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
package com.netflix.servo.monitor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class MonitorsTest {

    @Test
    public void testAddMonitorFields() throws Exception {
        List<Monitor<?>> monitors = Lists.newArrayList();
        ClassWithMonitors obj = new ClassWithMonitors();
        Monitors.addMonitorFields(monitors, null, obj);
        Monitors.addMonitorFields(monitors, "foo", obj);
        //System.out.println(monitors);
        assertEquals(monitors.size(), 8);

        Monitor<?> m = monitors.get(0);
        assertEquals(m.getConfig().getTags().getTag("class").getValue(), "ClassWithMonitors");
    }

    @Test
    public void testAddAnnotatedFields() throws Exception {
        List<Monitor<?>> monitors = Lists.newArrayList();
        ClassWithMonitors obj = new ClassWithMonitors();
        Monitors.addAnnotatedFields(monitors, null, obj);
        Monitors.addAnnotatedFields(monitors, "foo", obj);
        //System.out.println(monitors);
        assertEquals(monitors.size(), 6);
    }

    @Test
    public void testNewObjectMonitor() throws Exception {
        ClassWithMonitors obj = new ClassWithMonitors();
        List<Monitor<?>> monitors = Monitors.newObjectMonitor(obj).getMonitors();
        System.err.println(monitors);
        assertEquals(monitors.size(), 7);
    }
}
