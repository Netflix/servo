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
package com.netflix.servo.monitor;


import static org.testng.Assert.*;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

public class BucketTimerTest extends AbstractMonitorTest<BucketTimer> {

    @Override
    public BucketTimer newInstance(String name) {
        return new BucketTimer(
            MonitorConfig.builder(name).build(),
            new BucketConfig.Builder().withBuckets(new long[]{0L, 10L, 20L}).build()
        );
    }

    @Test
    public void testRecord() throws Exception {
        BucketTimer c = newInstance("foo");
        Map<String, Number> expectedValues;

        expectedValues = Maps.newHashMap();
        expectedValues.put("totalTime", 0L);
        expectedValues.put("count", 0L);
        expectedValues.put("min", 0L);
        expectedValues.put("max", 0L);
        expectedValues.put("bucketTime_0ms", 0L);
        expectedValues.put("bucketTime_10ms", 0L);
        expectedValues.put("bucketTime_20ms", 0L);
        expectedValues.put("bucketCount_0ms", 0L);
        expectedValues.put("bucketCount_10ms", 0L);
        expectedValues.put("bucketCount_20ms", 0L);
        assertMonitors(c.getMonitors(), expectedValues);

        c.record(40);

        expectedValues = Maps.newHashMap();
        expectedValues.put("totalTime", 40L);
        expectedValues.put("count", 1L);
        expectedValues.put("min", 40L);
        expectedValues.put("max", 40L);
        expectedValues.put("bucketTime_0ms", 0L);
        expectedValues.put("bucketTime_10ms", 0L);
        expectedValues.put("bucketTime_20ms", 40L);
        expectedValues.put("bucketCount_0ms", 0L);
        expectedValues.put("bucketCount_10ms", 0L);
        expectedValues.put("bucketCount_20ms", 1L);
        assertMonitors(c.getMonitors(), expectedValues);

        c.record(10);

        expectedValues = Maps.newHashMap();
        expectedValues.put("totalTime", 50L);
        expectedValues.put("count", 2L);
        expectedValues.put("min", 10L);
        expectedValues.put("max", 40L);
        expectedValues.put("bucketTime_0ms", 0L);
        expectedValues.put("bucketTime_10ms", 10L);
        expectedValues.put("bucketTime_20ms", 40L);
        expectedValues.put("bucketCount_0ms", 0L);
        expectedValues.put("bucketCount_10ms", 1L);
        expectedValues.put("bucketCount_20ms", 1L);
        assertMonitors(c.getMonitors(), expectedValues);

        c.record(5);

        expectedValues = Maps.newHashMap();
        expectedValues.put("totalTime", 55L);
        expectedValues.put("count", 3L);
        expectedValues.put("min", 5L);
        expectedValues.put("max", 40L);
        expectedValues.put("bucketTime_0ms", 5L);
        expectedValues.put("bucketTime_10ms", 10L);
        expectedValues.put("bucketTime_20ms", 40L);
        expectedValues.put("bucketCount_0ms", 1L);
        expectedValues.put("bucketCount_10ms", 1L);
        expectedValues.put("bucketCount_20ms", 1L);
        assertMonitors(c.getMonitors(), expectedValues);

        c.record(0);

        expectedValues = Maps.newHashMap();
        expectedValues.put("totalTime", 55L);
        expectedValues.put("count", 4L);
        expectedValues.put("min", 0L);
        expectedValues.put("max", 40L);
        expectedValues.put("bucketTime_0ms", 5L);
        expectedValues.put("bucketTime_10ms", 10L);
        expectedValues.put("bucketTime_20ms", 40L);
        expectedValues.put("bucketCount_0ms", 2L);
        expectedValues.put("bucketCount_10ms", 1L);
        expectedValues.put("bucketCount_20ms", 1L);
        assertMonitors(c.getMonitors(), expectedValues);

        c.record(45);

        expectedValues = Maps.newHashMap();
        expectedValues.put("totalTime", 100L);
        expectedValues.put("count", 5L);
        expectedValues.put("min", 0L);
        expectedValues.put("max", 45L);
        expectedValues.put("bucketTime_0ms", 5L);
        expectedValues.put("bucketTime_10ms", 10L);
        expectedValues.put("bucketTime_20ms", 85L);
        expectedValues.put("bucketCount_0ms", 2L);
        expectedValues.put("bucketCount_10ms", 1L);
        expectedValues.put("bucketCount_20ms", 2L);
        assertMonitors(c.getMonitors(), expectedValues);
    }

    @Test
    public void testRecordWithReverseCumulation() throws Exception {
        BucketTimer c = new BucketTimer(
            MonitorConfig.builder("foo").build(),
            new BucketConfig.Builder()
                .withBuckets(new long[]{0L, 10L, 20L})
                .setReverseCumulative(true)
                .build()
        );
        Map<String, Number> expectedValues;

        expectedValues = Maps.newHashMap();
        expectedValues.put("totalTime", 0L);
        expectedValues.put("count", 0L);
        expectedValues.put("min", 0L);
        expectedValues.put("max", 0L);
        expectedValues.put("bucketTimeRevCum_0ms", 0L);
        expectedValues.put("bucketTimeRevCum_10ms", 0L);
        expectedValues.put("bucketTimeRevCum_20ms", 0L);
        expectedValues.put("bucketCountRevCum_0ms", 0L);
        expectedValues.put("bucketCountRevCum_10ms", 0L);
        expectedValues.put("bucketCountRevCum_20ms", 0L);
        assertMonitors(c.getMonitors(), expectedValues);

        c.record(40);

        expectedValues = Maps.newHashMap();
        expectedValues.put("totalTime", 40L);
        expectedValues.put("count", 1L);
        expectedValues.put("min", 40L);
        expectedValues.put("max", 40L);
        expectedValues.put("bucketTimeRevCum_0ms", 40L);
        expectedValues.put("bucketTimeRevCum_10ms", 40L);
        expectedValues.put("bucketTimeRevCum_20ms", 40L);
        expectedValues.put("bucketCountRevCum_0ms", 1L);
        expectedValues.put("bucketCountRevCum_10ms", 1L);
        expectedValues.put("bucketCountRevCum_20ms", 1L);
        assertMonitors(c.getMonitors(), expectedValues);

        c.record(10);

        expectedValues = Maps.newHashMap();
        expectedValues.put("totalTime", 50L);
        expectedValues.put("count", 2L);
        expectedValues.put("min", 10L);
        expectedValues.put("max", 40L);
        expectedValues.put("bucketTimeRevCum_0ms", 50L);
        expectedValues.put("bucketTimeRevCum_10ms", 50L);
        expectedValues.put("bucketTimeRevCum_20ms", 40L);
        expectedValues.put("bucketCountRevCum_0ms", 2L);
        expectedValues.put("bucketCountRevCum_10ms", 2L);
        expectedValues.put("bucketCountRevCum_20ms", 1L);
        assertMonitors(c.getMonitors(), expectedValues);

        c.record(5);

        expectedValues = Maps.newHashMap();
        expectedValues.put("totalTime", 55L);
        expectedValues.put("count", 3L);
        expectedValues.put("min", 5L);
        expectedValues.put("max", 40L);
        expectedValues.put("bucketTimeRevCum_0ms", 55L);
        expectedValues.put("bucketTimeRevCum_10ms", 50L);
        expectedValues.put("bucketTimeRevCum_20ms", 40L);
        expectedValues.put("bucketCountRevCum_0ms", 3L);
        expectedValues.put("bucketCountRevCum_10ms", 2L);
        expectedValues.put("bucketCountRevCum_20ms", 1L);
        assertMonitors(c.getMonitors(), expectedValues);

        c.record(0);

        expectedValues = Maps.newHashMap();
        expectedValues.put("totalTime", 55L);
        expectedValues.put("count", 4L);
        expectedValues.put("min", 0L);
        expectedValues.put("max", 40L);
        expectedValues.put("bucketTimeRevCum_0ms", 55L);
        expectedValues.put("bucketTimeRevCum_10ms", 50L);
        expectedValues.put("bucketTimeRevCum_20ms", 40L);
        expectedValues.put("bucketCountRevCum_0ms", 4L);
        expectedValues.put("bucketCountRevCum_10ms", 2L);
        expectedValues.put("bucketCountRevCum_20ms", 1L);
        assertMonitors(c.getMonitors(), expectedValues);

        c.record(45);

        expectedValues = Maps.newHashMap();
        expectedValues.put("totalTime", 100L);
        expectedValues.put("count", 5L);
        expectedValues.put("min", 0L);
        expectedValues.put("max", 45L);
        expectedValues.put("bucketTimeRevCum_0ms", 100L);
        expectedValues.put("bucketTimeRevCum_10ms", 95L);
        expectedValues.put("bucketTimeRevCum_20ms", 85L);
        expectedValues.put("bucketCountRevCum_0ms", 5L);
        expectedValues.put("bucketCountRevCum_10ms", 3L);
        expectedValues.put("bucketCountRevCum_20ms", 2L);
        assertMonitors(c.getMonitors(), expectedValues);
    }

    private void assertMonitors(List<Monitor<?>> monitors, Map<String, Number> expectedValues) {
        for (Monitor<?> monitor : monitors) {
            final String bucket = monitor.getConfig().getTags().getValue("bucket");
            final Number actual = (Number) monitor.getValue();
            final Number expected = expectedValues.get(bucket);
            assertEquals(actual, expected, bucket);
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
