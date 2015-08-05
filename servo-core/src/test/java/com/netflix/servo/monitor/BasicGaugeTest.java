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

import org.testng.annotations.Test;

import java.util.concurrent.Callable;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class BasicGaugeTest extends AbstractMonitorTest<BasicGauge<Long>> {

  private static class TestFunc implements Callable<Long> {

    private final long value;

    public TestFunc(long v) {
      value = v;
    }

    public Long call() {
      return value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      TestFunc testFunc = (TestFunc) o;
      return value == testFunc.value;
    }

    @Override
    public int hashCode() {
      return (int) (value ^ (value >>> 32));
    }
  }

  public BasicGauge<Long> newInstance(String name) {
    long v = Long.parseLong(name);
    return new BasicGauge<>(MonitorConfig.builder(name).build(), new TestFunc(v));
  }

  @Test
  public void testGetValue() throws Exception {
    BasicGauge<Long> c = newInstance("42");
    assertEquals(c.getValue().longValue(), 42L);
  }

  @Test
  public void testEqualsCount() throws Exception {
    BasicGauge<Long> c1 = newInstance("42");
    BasicGauge<Long> c2 = newInstance("43");
    BasicGauge<Long> c3 = newInstance("43");
    assertNotEquals(c1, c2);
    assertEquals(c2, c3);
  }
}
