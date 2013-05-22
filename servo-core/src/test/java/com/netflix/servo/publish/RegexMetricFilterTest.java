/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.publish;

import com.google.common.collect.ImmutableList;
import com.netflix.servo.Metric;
import com.netflix.servo.tag.SortedTagList;
import org.testng.annotations.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.testng.Assert.assertEquals;

public class RegexMetricFilterTest {

    private List<Metric> mkList() {
        return ImmutableList.of(
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
        Pattern pattern = Pattern.compile("^a\\.b\\.c.*");
        MetricFilter filter = new RegexMetricFilter("c", pattern, false, false);
        MetricPoller poller = newPoller();

        List<Metric> metrics = poller.poll(filter);
        assertEquals(metrics.size(), 3);
        assertEquals(metrics.get(0), mkList().get(1));
    }

    @Test
    public void testPrefixFilterMatchIfTagMissing() throws Exception {
        Pattern pattern = Pattern.compile("^a\\.b\\.c.*");
        MetricFilter filter = new RegexMetricFilter("c", pattern, true, false);
        MetricPoller poller = newPoller();

        List<Metric> metrics = poller.poll(filter);
        assertEquals(metrics.size(), 4);
        assertEquals(metrics.get(0), mkList().get(0));
    }

    @Test
    public void testPrefixFilterInvert() throws Exception {
        Pattern pattern = Pattern.compile("^a\\.b\\.c.*");
        MetricFilter filter = new RegexMetricFilter("c", pattern, true, true);
        MetricPoller poller = newPoller();

        List<Metric> metrics = poller.poll(filter);
        assertEquals(metrics.size(), 1);
        assertEquals(metrics.get(0), mkList().get(4));
    }

    @Test
    public void testPrefixFilterName() throws Exception {
        Pattern pattern = Pattern.compile("m[13]");
        MetricFilter filter = new RegexMetricFilter(null, pattern, false, false);
        MetricPoller poller = newPoller();

        List<Metric> metrics = poller.poll(filter);
        assertEquals(metrics.size(), 2);
        assertEquals(metrics.get(0), mkList().get(0));
        assertEquals(metrics.get(1), mkList().get(2));
    }
}
