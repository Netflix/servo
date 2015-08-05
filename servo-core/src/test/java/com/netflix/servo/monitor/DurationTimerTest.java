/**
 * Copyright 2014 Netflix, Inc.
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

import com.netflix.servo.util.ManualClock;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

public class DurationTimerTest extends AbstractMonitorTest {
  @Override
  public Monitor<?> newInstance(String name) {
    return new DurationTimer(MonitorConfig.builder(name).build());
  }

  @SuppressWarnings("unchecked")
  private static Monitor<Long> getDuration(List<Monitor<?>> monitors) {
    return (Monitor<Long>) monitors.get(0);
  }

  @SuppressWarnings("unchecked")
  private static Monitor<Long> getActiveTasks(List<Monitor<?>> monitors) {
    return (Monitor<Long>) monitors.get(1);
  }

  @Test
  public void testGetMonitors() throws Exception {
    List<Monitor<?>> monitors = ((CompositeMonitor<?>) newInstance("test")).getMonitors();
    assertEquals(monitors.size(), 2);

    Monitor<Long> duration = getDuration(monitors);
    Monitor<Long> activeTasks = getActiveTasks(monitors);

    assertEquals(duration.getConfig().getName(), "test.duration");
    assertEquals(activeTasks.getConfig().getName(), "test.activeTasks");

    assertEquals(duration.getValue().longValue(), 0L);
    assertEquals(activeTasks.getValue().longValue(), 0L);
  }

  @Test
  public void testTimer() throws Exception {
    ManualClock clock = new ManualClock(0);
    DurationTimer timer = new DurationTimer(MonitorConfig.builder("test").build(), clock);
    Stopwatch s = timer.start();
    clock.set(10 * 1000L);
    assertEquals(10, s.getDuration());

    Monitor<Long> duration = getDuration(timer.getMonitors());
    Monitor<Long> activeTasks = getActiveTasks(timer.getMonitors());
    assertEquals(10L, duration.getValue().longValue());
    assertEquals(1L, activeTasks.getValue().longValue());
    clock.set(20 * 1000L);

    assertEquals(20L, duration.getValue().longValue());
    assertEquals(1L, activeTasks.getValue().longValue());

    Stopwatch anotherTask = timer.start();
    assertEquals(20L, duration.getValue().longValue());
    assertEquals(2L, activeTasks.getValue().longValue());

    clock.set(30 * 1000L);
    assertEquals(40L, duration.getValue().longValue()); // 30s for the first, 10s for the second
    assertEquals(2L, activeTasks.getValue().longValue());

    s.stop();
    assertEquals(10L, duration.getValue().longValue()); // 30s for the first, 10s for the second
    assertEquals(1L, activeTasks.getValue().longValue());

    anotherTask.stop();
    assertEquals(0L, duration.getValue().longValue());
    assertEquals(0L, activeTasks.getValue().longValue());
  }

  @Test
  public void testValue() throws Exception {
    ManualClock clock = new ManualClock(0L);
    DurationTimer timer = new DurationTimer(MonitorConfig.builder("test").build(), clock);
    assertEquals(0L, timer.getValue().longValue());
    Stopwatch s = timer.start();
    clock.set(10 * 1000L);
    assertEquals(10L, timer.getValue().longValue());
    s.stop();
    assertEquals(0L, timer.getValue().longValue());
  }

  @Test
  public void testReset() throws Exception {
    ManualClock clock = new ManualClock(0L);
    DurationTimer timer = new DurationTimer(MonitorConfig.builder("test").build(), clock);
    Stopwatch s = timer.start();
    clock.set(10 * 1000L);
    assertEquals(10L, timer.getValue().longValue());
    s.reset();
    assertEquals(0L, timer.getValue().longValue());
    clock.set(20 * 1000L);
    assertEquals(10L, timer.getValue().longValue());
  }
}
