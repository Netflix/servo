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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class BasicDistributionSummaryTest extends AbstractMonitorTest<BasicDistributionSummary> {
  public BasicDistributionSummary newInstance(String name) {
    return new BasicDistributionSummary(MonitorConfig.builder(name).build());
  }

  @Test
  public void testGetValue() throws Exception {
    BasicDistributionSummary m = newInstance("foo");
    // initial values
    assertEquals(m.getValue().longValue(), 0L);
    assertEquals(m.getCount().longValue(), 0L);
    assertEquals(m.getTotalAmount().longValue(), 0L);
    assertEquals(m.getMax().longValue(), 0L);
    assertEquals(m.getMin().longValue(), 0L);

    m.record(42);
    assertEquals(m.getValue().longValue(), 42L);
    assertEquals(m.getTotalAmount().longValue(), 42L);
    assertEquals(m.getCount().longValue(), 1L);
    assertEquals(m.getMax().longValue(), 42L);
    assertEquals(m.getMin().longValue(), 42L);

    m.record(21);
    assertEquals(m.getValue().longValue(), 31L);
    assertEquals(m.getTotalAmount().longValue(), 63L);
    assertEquals(m.getCount().longValue(), 2L);
    assertEquals(m.getMax().longValue(), 42L);
    assertEquals(m.getMin().longValue(), 21L);
  }

  @Test
  public void testRecord0() throws Exception {
    BasicDistributionSummary c = newInstance("foo");
    assertEquals(c.getCount().longValue(), 0L);

    c.record(42);
    assertEquals(c.getCount().longValue(), 1L);

    // Explicit 0 should be counted
    c.record(0);
    assertEquals(c.getCount().longValue(), 2L);

    // Negative values should be ignored
    c.record(-1);
    assertEquals(c.getCount().longValue(), 2L);
  }
}
