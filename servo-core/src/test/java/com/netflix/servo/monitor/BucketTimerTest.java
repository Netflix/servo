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


import com.netflix.servo.util.UnmodifiableSet;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class BucketTimerTest extends AbstractMonitorTest<BucketTimer> {

  @Override
  public BucketTimer newInstance(String name) {
    return new BucketTimer(
        MonitorConfig.builder(name).build(),
        new BucketConfig.Builder().withBuckets(new long[]{10L, 20L}).build()
    );
  }

  @Test
  public void testRecord() throws Exception {
    BucketTimer c = newInstance("foo");
    Map<String, Number> expectedValues;

    expectedValues = new HashMap<>();
    expectedValues.put("totalTime", 0L);
    expectedValues.put("min", 0L);
    expectedValues.put("max", 0L);
    expectedValues.put("bucket=10ms", 0L);
    expectedValues.put("bucket=20ms", 0L);
    expectedValues.put("bucket=overflow", 0L);
    assertMonitors(c.getMonitors(), expectedValues);
    assertEquals(c.getCount(0).longValue(), 0L);

    c.record(40);

    expectedValues = new HashMap<>();
    expectedValues.put("totalTime", 40L);
    expectedValues.put("min", 40L);
    expectedValues.put("max", 40L);
    expectedValues.put("bucket=10ms", 0L);
    expectedValues.put("bucket=20ms", 0L);
    expectedValues.put("bucket=overflow", 1L);
    assertMonitors(c.getMonitors(), expectedValues);
    assertEquals(c.getCount(0).longValue(), 1L);

    c.record(10);

    expectedValues = new HashMap<>();
    expectedValues.put("totalTime", 50L);
    expectedValues.put("min", 10L);
    expectedValues.put("max", 40L);
    expectedValues.put("bucket=10ms", 1L);
    expectedValues.put("bucket=20ms", 0L);
    expectedValues.put("bucket=overflow", 1L);
    assertMonitors(c.getMonitors(), expectedValues);
    assertEquals(c.getCount(0).longValue(), 2L);

    c.record(5);

    expectedValues = new HashMap<>();
    expectedValues.put("totalTime", 55L);
    expectedValues.put("min", 5L);
    expectedValues.put("max", 40L);
    expectedValues.put("bucket=10ms", 2L);
    expectedValues.put("bucket=20ms", 0L);
    expectedValues.put("bucket=overflow", 1L);
    assertMonitors(c.getMonitors(), expectedValues);
    assertEquals(c.getCount(0).longValue(), 3L);

    c.record(20);

    expectedValues = new HashMap<>();
    expectedValues.put("totalTime", 75L);
    expectedValues.put("min", 5L);
    expectedValues.put("max", 40L);
    expectedValues.put("bucket=10ms", 2L);
    expectedValues.put("bucket=20ms", 1L);
    expectedValues.put("bucket=overflow", 1L);
    assertMonitors(c.getMonitors(), expectedValues);
    assertEquals(c.getCount(0).longValue(), 4L);

    c.record(125);

    expectedValues = new HashMap<>();
    expectedValues.put("totalTime", 200L);
    expectedValues.put("min", 5L);
    expectedValues.put("max", 125L);
    expectedValues.put("bucket=10ms", 2L);
    expectedValues.put("bucket=20ms", 1L);
    expectedValues.put("bucket=overflow", 2L);
    assertMonitors(c.getMonitors(), expectedValues);
    assertEquals(c.getCount(0).longValue(), 5L);
  }

  @Test
  public void testRecordDifferentUnits() throws Exception {
    BucketTimer c = new BucketTimer(
        MonitorConfig.builder("foo").build(),
        new BucketConfig.Builder().withBuckets(new long[]{10000L, 20000L}).withTimeUnit(TimeUnit.NANOSECONDS).build(),
        TimeUnit.MICROSECONDS
    );;
    Map<String, Number> expectedValues;

    expectedValues = new HashMap<>();
    expectedValues.put("totalTime", 0L);
    expectedValues.put("min", 0L);
    expectedValues.put("max", 0L);
    expectedValues.put("bucket=10000ns", 0L);
    expectedValues.put("bucket=20000ns", 0L);
    expectedValues.put("bucket=overflow", 0L);
    assertMonitors(c.getMonitors(), expectedValues);
    assertEquals(c.getCount(0).longValue(), 0L);

    c.record(40);

    expectedValues = new HashMap<>();
    expectedValues.put("totalTime", 40L);
    expectedValues.put("min", 40L);
    expectedValues.put("max", 40L);
    expectedValues.put("bucket=10000ns", 0L);
    expectedValues.put("bucket=20000ns", 0L);
    expectedValues.put("bucket=overflow", 1L);
    assertMonitors(c.getMonitors(), expectedValues);
    assertEquals(c.getCount(0).longValue(), 1L);

    c.record(10);

    expectedValues = new HashMap<>();
    expectedValues.put("totalTime", 50L);
    expectedValues.put("min", 10L);
    expectedValues.put("max", 40L);
    expectedValues.put("bucket=10000ns", 1L);
    expectedValues.put("bucket=20000ns", 0L);
    expectedValues.put("bucket=overflow", 1L);
    assertMonitors(c.getMonitors(), expectedValues);
    assertEquals(c.getCount(0).longValue(), 2L);

    c.record(5);

    expectedValues = new HashMap<>();
    expectedValues.put("totalTime", 55L);
    expectedValues.put("min", 5L);
    expectedValues.put("max", 40L);
    expectedValues.put("bucket=10000ns", 2L);
    expectedValues.put("bucket=20000ns", 0L);
    expectedValues.put("bucket=overflow", 1L);
    assertMonitors(c.getMonitors(), expectedValues);
    assertEquals(c.getCount(0).longValue(), 3L);

    c.record(20);

    expectedValues = new HashMap<>();
    expectedValues.put("totalTime", 75L);
    expectedValues.put("min", 5L);
    expectedValues.put("max", 40L);
    expectedValues.put("bucket=10000ns", 2L);
    expectedValues.put("bucket=20000ns", 1L);
    expectedValues.put("bucket=overflow", 1L);
    assertMonitors(c.getMonitors(), expectedValues);
    assertEquals(c.getCount(0).longValue(), 4L);

    c.record(125);

    expectedValues = new HashMap<>();
    expectedValues.put("totalTime", 200L);
    expectedValues.put("min", 5L);
    expectedValues.put("max", 125L);
    expectedValues.put("bucket=10000ns", 2L);
    expectedValues.put("bucket=20000ns", 1L);
    expectedValues.put("bucket=overflow", 2L);
    assertMonitors(c.getMonitors(), expectedValues);
    assertEquals(c.getCount(0).longValue(), 5L);
  }

  private void assertMonitors(List<Monitor<?>> monitors, Map<String, Number> expectedValues) {
    Set<String> exclude = UnmodifiableSet.of("count", "min", "max");
    String[] namespaces = new String[]{"statistic", "servo.bucket"};
    for (Monitor<?> monitor : monitors) {
      for (String namespace : namespaces) {
        final String tag = monitor.getConfig().getTags().getValue(namespace);
        if (tag != null && !exclude.contains(tag)) {
          final Number actual = (Number) monitor.getValue();
          final Number expected = expectedValues.get(tag);
          assertEquals(actual, expected, namespace + "." + tag);
        }
      }
    }
  }

  @Test
  public void testEqualsCount() throws Exception {
    BucketTimer c1 = newInstance("foo");
    BucketTimer c2 = newInstance("foo");
    assertEquals(c1, c2);

    c1.record(42);
    assertNotEquals(c1, c2);
    c2.record(42);
    assertEquals(c1, c2);

    c1.record(11);
    assertNotEquals(c1, c2);
    c2.record(11);
    assertEquals(c1, c2);
  }

  @Test
  public void testHashCode() throws Exception {
    BucketTimer c1 = newInstance("foo");
    BucketTimer c2 = newInstance("foo");
    assertEquals(c1.hashCode(), c2.hashCode());

    c1.record(42);
    assertNotEquals(c1.hashCode(), c2.hashCode());
    c2.record(42);
    assertEquals(c1.hashCode(), c2.hashCode());

    c1.record(11);
    assertNotEquals(c1.hashCode(), c2.hashCode());
    c2.record(11);
    assertEquals(c1.hashCode(), c2.hashCode());
  }
}
