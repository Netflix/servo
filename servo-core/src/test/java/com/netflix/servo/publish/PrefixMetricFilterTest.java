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
package com.netflix.servo.publish;

import com.netflix.servo.Metric;
import com.netflix.servo.tag.SortedTagList;
import com.netflix.servo.util.UnmodifiableList;
import org.testng.annotations.Test;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static com.netflix.servo.publish.BasicMetricFilter.MATCH_ALL;
import static com.netflix.servo.publish.BasicMetricFilter.MATCH_NONE;
import static org.testng.Assert.assertEquals;

public class PrefixMetricFilterTest {

  private List<Metric> mkList() {
    return UnmodifiableList.of(
        new Metric("m1", SortedTagList.EMPTY, 0L, 0),
        new Metric("m2", SortedTagList.builder().withTag("c", "a.b.c.d.M1").build(), 0L, 0),
        new Metric("m3", SortedTagList.builder().withTag("c", "a.b.c.c.M3").build(), 0L, 0),
        new Metric("m4", SortedTagList.builder().withTag("c", "a.b.c.d.M4").build(), 0L, 0),
        new Metric("m5", SortedTagList.builder().withTag("c", "a.a.a.a.M5").build(), 0L, 0)
    );
  }

  private MetricPoller newPoller() {
    MockMetricPoller poller = new MockMetricPoller();
    poller.setMetrics(mkList());
    return poller;
  }

  @Test
  public void testPrefixFilter() throws Exception {
    NavigableMap<String, MetricFilter> filters = new TreeMap<>();
    filters.put("a.b.c", MATCH_ALL);
    MetricFilter filter = new PrefixMetricFilter("c", MATCH_NONE, filters);
    MetricPoller poller = newPoller();

    List<Metric> metrics = poller.poll(filter);
    assertEquals(metrics.size(), 3);
  }

  @Test
  public void testLongestPrefixFilter() throws Exception {
    NavigableMap<String, MetricFilter> filters = new TreeMap<>();
    filters.put("a.b.c", MATCH_ALL);
    filters.put("a.b.c.c", MATCH_NONE);
    MetricFilter filter = new PrefixMetricFilter("c", MATCH_NONE, filters);
    MetricPoller poller = newPoller();

    List<Metric> metrics = poller.poll(filter);
    assertEquals(metrics.size(), 2);
  }

  @Test
  public void testPrefixFilterOnName() throws Exception {
    NavigableMap<String, MetricFilter> filters = new TreeMap<>();
    filters.put("m", MATCH_ALL);
    MetricFilter filter = new PrefixMetricFilter(null, MATCH_NONE, filters);
    MetricPoller poller = newPoller();

    List<Metric> metrics = poller.poll(filter);
    assertEquals(metrics.size(), 5);
  }
}
