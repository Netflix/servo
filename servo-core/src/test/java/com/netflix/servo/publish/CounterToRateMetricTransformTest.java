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
import com.google.common.collect.Maps;
import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.tag.SortedTagList;
import com.netflix.servo.tag.TagList;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CounterToRateMetricTransformTest {

    private static final TagList GAUGE = SortedTagList.builder()
        .withTag(DataSourceType.GAUGE)
        .build();

    private static final TagList COUNTER = SortedTagList.builder()
        .withTag(DataSourceType.COUNTER)
        .build();

    private List<Metric> mkList(long ts, int value) {
        return ImmutableList.of(
            new Metric("m1", SortedTagList.EMPTY, ts, value),
            new Metric("m2", GAUGE, ts, value),
            new Metric("m3", COUNTER, ts, value)
        );
    }

    private Map<String, Double> mkMap(List<List<Metric>> updates) {
        Map<String, Double> map = Maps.newHashMap();
        for (Metric m : updates.get(0)) {
            map.put(m.getConfig().getName(), m.getNumberValue().doubleValue());
        }
        return map;
    }

    @Test
    public void testSimpleRate() throws Exception {
        MemoryMetricObserver mmo = new MemoryMetricObserver("m", 1);
        MetricObserver transform = new CounterToRateMetricTransform(mmo, 120, TimeUnit.SECONDS);
        Map<String, Double> metrics;

        // Make time look like the future to avoid expirations
        long baseTime = System.currentTimeMillis() + 100000L;

        // First sample
        transform.update(mkList(baseTime + 0, 0));
        metrics = mkMap(mmo.getObservations());
        assertEquals(metrics.size(), 2);
        assertTrue(metrics.get("m3") == null);

        // Delta of 5 in 5 seconds
        transform.update(mkList(baseTime + 5000, 5));
        metrics = mkMap(mmo.getObservations());
        assertEquals(metrics.size(), 3);
        assertEquals(metrics.get("m3"), 1.0, 0.00001);

        // Delta of 15 in 5 seconds
        transform.update(mkList(baseTime + 10000, 20));
        metrics = mkMap(mmo.getObservations());
        assertEquals(metrics.size(), 3);
        assertEquals(metrics.get("m3"), 3.0, 0.00001);

        // No change from previous sample
        transform.update(mkList(baseTime + 15000, 20));
        metrics = mkMap(mmo.getObservations());
        assertEquals(metrics.size(), 3);
        assertEquals(metrics.get("m3"), 0.0, 0.00001);

        // Decrease from previous sample
        transform.update(mkList(baseTime + 20000, 19));
        metrics = mkMap(mmo.getObservations());
        assertEquals(metrics.size(), 3);
        assertEquals(metrics.get("m3"), 0.0, 0.00001);
    }

    @Test
    public void testFirstSample() throws Exception {
        MemoryMetricObserver mmo = new MemoryMetricObserver("m", 1);
        MetricObserver transform = new CounterToRateMetricTransform(mmo, 120, 5, TimeUnit.SECONDS);
        Map<String, Double> metrics;

        // Make time look like the future to avoid expirations
        long baseTime = System.currentTimeMillis() + 100000L;

        // First sample
        transform.update(mkList(baseTime + 0, 10));
        metrics = mkMap(mmo.getObservations());
        assertEquals(metrics.size(), 3);
        assertEquals(metrics.get("m3"), 2.0, 0.00001);

        // Delta of 5 in 5 seconds
        transform.update(mkList(baseTime + 5000, 15));
        metrics = mkMap(mmo.getObservations());
        assertEquals(metrics.size(), 3);
        assertEquals(metrics.get("m3"), 1.0, 0.00001);
    }
}
