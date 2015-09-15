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

import com.netflix.servo.stats.StatsConfig;
import com.netflix.servo.util.ManualClock;
import org.testng.annotations.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class StatsMonitorTest {

  @Test
  public void testExpiration() throws Exception {
    ManualClock clock = new ManualClock(0);
    StatsMonitor monitor = new StatsMonitor(MonitorConfig.builder("m1").build(),
        new StatsConfig.Builder().withComputeFrequencyMillis(1).build(),
        Executors.newSingleThreadScheduledExecutor(),
        "total", false, clock);

    monitor.startComputingStats();

    clock.set(TimeUnit.MINUTES.toMillis(20));
    Thread.sleep(20);
    assertTrue(monitor.isExpired());
    monitor.getMonitors();
    Thread.sleep(20);
    assertFalse(monitor.isExpired());
  }
}