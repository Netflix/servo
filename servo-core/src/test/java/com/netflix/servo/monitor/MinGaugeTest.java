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

import com.netflix.servo.util.ManualClock;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class MinGaugeTest extends AbstractMonitorTest<MinGauge> {

  private final ManualClock clock = new ManualClock(0L);

  @Override
  public MinGauge newInstance(String name) {
    MonitorConfig config = MonitorConfig.builder(name).build();
    return new MinGauge(config, clock);
  }

  @Test
  public void testUpdate() throws Exception {
    clock.set(0L);
    MinGauge minGauge = newInstance("min1");
    minGauge.update(42L);
    clock.set(60000L);
    assertEquals(minGauge.getValue().longValue(), 42L);
  }

  @Test
  public void testUpdate2() throws Exception {
    clock.set(0L);
    MinGauge minGauge = newInstance("min1");
    minGauge.update(42L);
    minGauge.update(420L);
    clock.set(60000L);
    assertEquals(minGauge.getValue().longValue(), 42L);
  }

  @Test
  public void testUpdate3() throws Exception {
    clock.set(0L);
    MinGauge minGauge = newInstance("min1");
    minGauge.update(42L);
    minGauge.update(420L);
    minGauge.update(1L);
    clock.set(60000L);
    assertEquals(minGauge.getValue().longValue(), 1L);
  }
}
