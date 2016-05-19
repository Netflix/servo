/**
 * Copyright 2015 Netflix, Inc.
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
import com.netflix.servo.util.ManualClock;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class DoubleCounterTest {

  private static final double DELTA = 1e-06;
  final ManualClock clock = new ManualClock(Pollers.POLLING_INTERVALS[1]);

  public DoubleCounter newInstance(String name) {
    return new DoubleCounter(MonitorConfig.builder(name).build(), clock);
  }

  private long time(long t) {
    return t * 1000 + Pollers.POLLING_INTERVALS[1];
  }

  @Test
  public void testIncrement() {
    assertEquals(Pollers.POLLING_INTERVALS[0], 60000L);
    DoubleCounter c = newInstance("c");
    c.increment(1.0);
    assertEquals(c.getCurrentCount(0), 1.0);
  }

  @Test
  public void testSimpleTransition() {
    clock.set(time(1));
    DoubleCounter c = newInstance("c");
    assertEquals(c.getValue(1).doubleValue(), 0.0, DELTA);
    assertEquals(c.getCurrentCount(1), 0.0);

    clock.set(time(3));
    c.increment(1);
    assertEquals(c.getValue(1).doubleValue(), 0.0, DELTA);
    assertEquals(c.getCurrentCount(1), 1.0);

    clock.set(time(6));
    c.increment(1);
    assertEquals(c.getValue(1).doubleValue(), 0.0, DELTA);
    assertEquals(c.getCurrentCount(1), 2.0);

    clock.set(time(12));
    c.increment(1);
    assertEquals(c.getCurrentCount(1), 1.0);
    assertEquals(c.getValue(1).doubleValue(), 2.0 / 10.0);
  }


  @Test
  public void testInitialPollIsZero() {
    clock.set(time(1));
    DoubleCounter c = newInstance("foo");
    assertEquals(c.getValue(1).doubleValue(), 0.0);
  }

  @Test
  public void testHasRightType() throws Exception {
    assertEquals(newInstance("foo").getConfig().getTags().getValue(DataSourceType.KEY),
        "NORMALIZED");
  }

  @Test
  public void testBoundaryTransition() {
    clock.set(time(1));
    DoubleCounter c = newInstance("foo");

    // Should all go to one bucket
    c.increment(1);
    clock.set(time(4));
    c.increment(1);
    clock.set(time(9));
    c.increment(1);

    // Should cause transition
    clock.set(time(10));
    c.increment(1);
    clock.set(time(19));
    c.increment(1);

    // Check counts
    assertEquals(c.getValue(1).doubleValue(), 0.3);
    assertEquals(c.getCurrentCount(1), 2.0);
  }

  @Test
  public void testResetPreviousValue() {
    clock.set(time(1));
    DoubleCounter c = newInstance("foo");
    for (int i = 1; i <= 100000; ++i) {
      c.increment(1);
      clock.set(time(i * 10 + 1));
      assertEquals(c.getValue(1).doubleValue(), 0.1);
    }
  }

  @Test
  public void testNonMonotonicClock() {
    clock.set(time(1));
    DoubleCounter c = newInstance("foo");
    c.getValue(1);

    c.increment(1);
    c.increment(1);
    clock.set(time(10));
    c.increment(1);
    clock.set(time(9)); // Should get ignored
    c.increment(1);
    assertEquals(c.getCurrentCount(1), 2.0);
    c.increment(1);
    clock.set(time(10));
    c.increment(1);
    c.increment(1);
    assertEquals(c.getCurrentCount(1), 5.0);

    // Check rate for previous interval
    assertEquals(c.getValue(1).doubleValue(), 0.2);
  }

  @Test
  public void testGetValueTwice() {
    ManualClock manualClock = new ManualClock(0L);

    DoubleCounter c = new DoubleCounter(MonitorConfig.builder("test").build(), manualClock);
    c.increment(1);
    for (int i = 1; i < 10; ++i) {
      manualClock.set(i * 60000L);
      c.increment(1);
      c.getValue(0);
      assertEquals(c.getValue(0).doubleValue(), 1 / 60.0);
    }
  }
}
