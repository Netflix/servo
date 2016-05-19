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
import com.netflix.servo.util.ManualClock;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class StepCounterTest {

  private static final double DELTA = 1e-06;
  private final ManualClock clock = new ManualClock(50 * Pollers.POLLING_INTERVALS[1]);

  public StepCounter newInstance(String name) {
    return new StepCounter(MonitorConfig.builder(name).build(), clock);
  }

  private long time(long t) {
    return t * 1000 + Pollers.POLLING_INTERVALS[1];
  }

  @Test
  public void testSimpleTransition() {
    long start = time(1);
    clock.set(start);
    StepCounter c = newInstance("c");
    assertEquals(c.getValue(1).doubleValue(), 0.0, DELTA);
    assertEquals(c.getCurrentCount(1), 0L);

    clock.set(start + 2000);
    c.increment();
    assertEquals(c.getValue(1).doubleValue(), 0.0, DELTA);
    assertEquals(c.getCurrentCount(1), 1L);

    clock.set(start + 8000);
    c.increment();
    assertEquals(c.getValue(1).doubleValue(), 0.0, DELTA);
    assertEquals(c.getCurrentCount(1), 2L);

    clock.set(start + 12000);
    c.increment();
    assertEquals(c.getValue(1).doubleValue(), 2.0 / 10.0, DELTA);
    assertEquals(c.getCurrentCount(1), 1L);
  }


  @Test
  public void testInitialPollIsZero() {
    clock.set(time(1));
    StepCounter c = newInstance("foo");
    assertEquals(c.getValue(1).doubleValue(), 0.0, DELTA);
  }

  @Test
  public void testHasRightType() throws Exception {
    assertEquals(newInstance("foo").getConfig().getTags().getValue(DataSourceType.KEY),
        "NORMALIZED");
  }

  @Test
  public void testBoundaryTransition() {
    clock.set(time(1));
    StepCounter c = newInstance("foo");

    // Should all go to one bucket
    c.increment();
    clock.set(time(4));
    c.increment();
    clock.set(time(9));
    c.increment();

    // Should cause transition
    clock.set(time(10));
    c.increment();
    clock.set(time(19));
    c.increment();

    // Check counts
    assertEquals(c.getValue(1).doubleValue(), 0.3, DELTA);
    assertEquals(c.getCurrentCount(1), 2);
  }

  @Test
  public void testResetPreviousValue() {
    clock.set(time(1));
    StepCounter c = newInstance("foo");
    for (int i = 1; i <= 100000; ++i) {
      c.increment();
      clock.set(time(i * 10 + 1));
      assertEquals(c.getValue(1).doubleValue(), 0.1, DELTA);
    }
  }

  @Test
  public void testMissedInterval() {
    clock.set(time(1));
    StepCounter c = newInstance("foo");
    c.getValue(1);

    // Multiple updates without polling
    c.increment(1);
    clock.set(time(4));
    c.increment(1);
    clock.set(time(14));
    c.increment(1);
    clock.set(time(24));
    c.increment(1);
    clock.set(time(34));
    c.increment(1);

    // Check counts
    assertEquals(c.getValue(1).doubleValue(), 0.1);
    assertEquals(c.getCurrentCount(1), 1);
  }

  @Test
  public void testMissedIntervalNoIncrements() {
    clock.set(time(1));
    StepCounter c = newInstance("foo");
    c.getValue(1);

    // Gaps without polling
    c.increment(5);
    clock.set(time(34));

    // Check counts, previous and current interval should be 0
    assertEquals(c.getValue(1).doubleValue(), 0.0);
    assertEquals(c.getCurrentCount(1), 0);
  }

  @Test
  public void testNonMonotonicClock() {
    clock.set(time(1));
    StepCounter c = newInstance("foo");
    c.getValue(1);

    c.increment();
    c.increment();
    clock.set(time(10));
    c.increment();
    clock.set(time(9)); // Should get ignored
    c.increment();
    assertEquals(c.getCurrentCount(1), 2);
    c.increment();
    clock.set(time(10));
    c.increment();
    c.increment();
    assertEquals(c.getCurrentCount(1), 5);

    // Check rate for previous interval
    assertEquals(c.getValue(1).doubleValue(), 0.2, DELTA);
  }

  @Test
  public void testGetValueTwice() {
    ManualClock manualClock = new ManualClock(60000L);

    StepCounter c = new StepCounter(MonitorConfig.builder("test").build(), manualClock);
    c.increment();
    for (int i = 2; i < 10; ++i) {
      manualClock.set(i * 60000L);
      c.increment();
      c.getValue(0);
      assertEquals(c.getValue(0).doubleValue(), 1 / 60.0, DELTA);
    }
  }

  @Test
  public void testIncrAfterMissedSteps() {
    clock.set(time(1));
    StepCounter c = newInstance("foo");
    c.increment();

    clock.set(time(11));
    assertEquals(c.getValue(1).doubleValue(), 0.1, DELTA);

    clock.set(time(31));
    c.increment();
    c.increment();
    c.increment();
    clock.set(time(41));
    assertEquals(c.getValue(1).doubleValue(), 0.3, DELTA);
  }
}
