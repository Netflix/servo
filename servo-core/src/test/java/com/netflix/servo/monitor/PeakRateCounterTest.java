/**
 * Copyright 2013 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.netflix.servo.monitor;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.util.ManualClock;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PeakRateCounterTest extends AbstractMonitorTest<PeakRateCounter> {
  final ManualClock clock = new ManualClock(0);

  @Override
  public PeakRateCounter newInstance(String name) {
    return new PeakRateCounter(MonitorConfig.builder(name).build(), clock);
  }

  @Test
  public void testIncrement() throws Exception {
    PeakRateCounter c = newInstance("foo");

    assertEquals(c.getValue().longValue(), 0L);


    for (int i = 0; i < 5; i++) {
      clock.set(i * 1000L);
      c.increment();
    }

    assertEquals(c.getValue().longValue(), 1L,
        "Delta of 5 in 5 seconds, e.g. peak rate = average, 1 per second");


    for (int i = 0; i < 5; i++) {
      clock.set((5 + i) * 1000L);
      c.increment(3);
    }

    assertEquals(c.getValue().longValue(), 3L,
        "Delta of 15 in 5 seconds, e.g. peak rate = average, 3 per second");


    clock.set(10 * 1000L);
    c.increment(10);
    for (int i = 0; i < 3; i++) {
      clock.set((11 + i) * 1000L);
      c.increment(3);
    }
    c.increment();

    assertEquals(c.getValue().longValue(), 10L,
        "Delta of 15 in 5 seconds, e.g. peak rate = 10, average = 3, min = 1 per second");

    clock.set(19 * 1000L);
    assertEquals(c.getValue().longValue(), 10L,
        "Delta of 0 in 5 seconds, e.g. peak rate = previous max, 10 per second");


  }

  @Test
  public void testHasRightType() throws Exception {
    Tag type = newInstance("foo").getConfig().getTags().getTag(DataSourceType.KEY);
    assertEquals(type.getValue(), "GAUGE");
  }

  @Test
  public void testEqualsAndHashCodeName() throws Exception {
    PeakRateCounter c1 = newInstance("1234567890");
    PeakRateCounter c2 = newInstance("1234567890");
    assertEquals(c1, c2);
    assertEquals(c1.hashCode(), c2.hashCode());
    c2 = c1;
    assertEquals(c2, c1);
  }
}
