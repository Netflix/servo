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
package com.netflix.servo.monitor;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class MonitorsTest {

  @Test
  public void testAddMonitorsAnon() throws Exception {
    List<Monitor<?>> monitors = new ArrayList<>();
    ClassWithMonitors obj = new ClassWithMonitors() {
      final Counter c1 = Monitors.newCounter("publicCounter");
      @com.netflix.servo.annotations.Monitor(
          name = "primitiveGauge", type = DataSourceType.GAUGE)
      static final long A1 = 0L;
    };
    TagList tags = BasicTagList.of("abc", "def");
    Monitors.addMonitors(monitors, null, tags, obj);

    assertEquals(monitors.size(), 10);
    for (Monitor m : monitors) {
      assertEquals(m.getConfig().getTags().getValue("class"), "MonitorsTest",
          String.format("%s should have class MonitorsTest", m.getConfig().getName()));
    }
  }

  @Test
  public void testAddMonitorFields() throws Exception {
    List<Monitor<?>> monitors = new ArrayList<>();
    ClassWithMonitors obj = new ClassWithMonitors();

    TagList tags = BasicTagList.of("abc", "def");

    Monitors.addMonitorFields(monitors, null, tags, obj, obj.getClass());
    Monitors.addMonitorFields(monitors, "foo", null, obj, obj.getClass());
    //System.out.println(monitors);
    assertEquals(monitors.size(), 8);

    Monitor<?> m = monitors.get(0);
    assertEquals(m.getConfig().getTags().getTag("class").getValue(), "ClassWithMonitors");
    assertEquals(m.getConfig().getTags().getTag("abc").getValue(), "def");
  }

  @Test
  public void testAddAnnotatedFields() throws Exception {
    List<Monitor<?>> monitors = new ArrayList<>();
    ClassWithMonitors obj = new ClassWithMonitors();
    Monitors.addAnnotatedFields(monitors, null, null, obj, obj.getClass());
    Monitors.addAnnotatedFields(monitors, "foo", null, obj, obj.getClass());
    //System.out.println(monitors);
    assertEquals(monitors.size(), 8);
  }

  @Test
  public void testNewObjectMonitor() throws Exception {
    ClassWithMonitors obj = new ClassWithMonitors();
    List<Monitor<?>> monitors = Monitors.newObjectMonitor(obj).getMonitors();
    //System.err.println(monitors);
    assertEquals(monitors.size(), 8);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNewObjectMonitorWithBadAnnotation() throws Exception {
    ClassWithBadAnnotation obj = new ClassWithBadAnnotation();
    Monitors.newObjectMonitor(obj);
  }

  @Test
  public void testNewObjectMonitorWithParentClass() throws Exception {
    ParentHasMonitors obj = new ParentHasMonitors();
    List<Monitor<?>> monitors = Monitors.newObjectMonitor(obj).getMonitors();
    for (Monitor m : monitors) {
      assertEquals(m.getConfig().getTags().getValue("class"), "ParentHasMonitors",
          String.format("%s should have class ParentHasMonitors", m.getConfig().getName()));
    }
    assertEquals(monitors.size(), 10);
  }

  @Test
  public void testIsRegistered() throws Exception {
    ClassWithMonitors obj = new ClassWithMonitors();
    assertFalse(Monitors.isObjectRegistered("id", obj));
    Monitors.registerObject("id", obj);
    assertTrue(Monitors.isObjectRegistered("id", obj));
  }

  @Test
  public void testNewObjectConfig() throws Exception {
    ClassWithMonitors obj = new ClassWithMonitors() {
    };
    List<Monitor<?>> monitors = Monitors.newObjectMonitor(obj).getMonitors();
    for (Monitor m : monitors) {
      assertEquals(m.getConfig().getTags().getValue("class"), "MonitorsTest",
          String.format("%s should have class MonitorsTest", m.getConfig().getName()));
    }
    assertEquals(monitors.size(), 8);
  }
}
