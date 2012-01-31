/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 - 2012 Netflix
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.netflix.servo.publish;

import static com.netflix.servo.BasicTagList.EMPTY;
import static com.netflix.servo.publish.BasicMetricFilter.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import com.netflix.servo.BasicTagList;
import com.netflix.servo.Metric;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class PrefixMetricFilterTest {

    private List<Metric> mkList() {
        return ImmutableList.of(
            new Metric("m1", EMPTY, 0L, 0),
            new Metric("m2", BasicTagList.copyOf("c=a.b.c.d.M1"), 0L, 0),
            new Metric("m3", BasicTagList.copyOf("c=a.b.c.c.M3"), 0L, 0),
            new Metric("m4", BasicTagList.copyOf("c=a.b.c.d.M4"), 0L, 0),
            new Metric("m5", BasicTagList.copyOf("c=a.a.a.a.M5"), 0L, 0)
        );
    }

    private MetricPoller newPoller() {
        MockMetricPoller poller = new MockMetricPoller();
        poller.setMetrics(mkList());
        return poller;
    }

    @Test
    public void testPrefixFilter() throws Exception {
        TreeMap<String,MetricFilter> filters = Maps.newTreeMap();
        filters.put("a.b.c", MATCH_ALL);
        MetricFilter filter = new PrefixMetricFilter("c", MATCH_NONE, filters);
        MetricPoller poller = newPoller();
      
        List<Metric> metrics = poller.poll(filter);
        assertEquals(metrics.size(), 3);
    }

    @Test
    public void testLongestPrefixFilter() throws Exception {
        TreeMap<String,MetricFilter> filters = Maps.newTreeMap();
        filters.put("a.b.c", MATCH_ALL);
        filters.put("a.b.c.c", MATCH_NONE);
        MetricFilter filter = new PrefixMetricFilter("c", MATCH_NONE, filters);
        MetricPoller poller = newPoller();
      
        List<Metric> metrics = poller.poll(filter);
        assertEquals(metrics.size(), 2);
    }

    @Test
    public void testPrefixFilterOnName() throws Exception {
        TreeMap<String,MetricFilter> filters = Maps.newTreeMap();
        filters.put("m", MATCH_ALL);
        MetricFilter filter = new PrefixMetricFilter(null, MATCH_NONE, filters);
        MetricPoller poller = newPoller();
      
        List<Metric> metrics = poller.poll(filter);
        assertEquals(metrics.size(), 5);
    }
}
